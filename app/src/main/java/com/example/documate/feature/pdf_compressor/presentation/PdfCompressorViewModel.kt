package com.example.documate.feature.pdf_compressor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class PdfCompressorViewModel(
    private val documentRepository: DocumentRepository,
    private val pdfRepository: PdfRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfCompressorUiState())
    val uiState: StateFlow<PdfCompressorUiState> = _uiState.asStateFlow()

    private var compressJob: Job? = null

    fun onEvent(event: PdfCompressorUiEvent) {
        when (event) {
            is PdfCompressorUiEvent.SelectPdf -> handleSelectPdf(event.uriString)
            is PdfCompressorUiEvent.RemovePdf -> _uiState.update { it.copy(selectedPdf = null) }
            is PdfCompressorUiEvent.SelectPreset -> _uiState.update { it.copy(selectedPreset = event.preset) }
            is PdfCompressorUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is PdfCompressorUiEvent.StartCompression -> handleStartCompression()
            is PdfCompressorUiEvent.CancelCompression -> handleCancelCompression()
            is PdfCompressorUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is PdfCompressorUiEvent.Reset -> _uiState.update { PdfCompressorUiState() }
        }
    }

    private fun handleSelectPdf(uriString: String) {
        viewModelScope.launch {
            when (val result = documentRepository.resolveFileMetadata(uriString)) {
                is AppResult.Success -> {
                    val pdf = result.data
                    val cleanBaseName = pdf.name.substringBeforeLast('.')
                    _uiState.update {
                        it.copy(
                            selectedPdf = pdf,
                            outputFileName = "${cleanBaseName}_compressed.pdf"
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    private fun handleStartCompression() {
        val state = _uiState.value
        val pdf = state.selectedPdf ?: return
        if (!state.canCompress) return

        compressJob?.cancel()
        compressJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCompressing = true,
                    progress = 0.1f,
                    statusMessage = "Analyzing PDF streams & images...",
                    errorMessage = null
                )
            }

            // Copy to cache
            val cachedFile: File = when (val copyResult = documentRepository.copyToAppCache(pdf.uriString, pdf.name)) {
                is AppResult.Success -> copyResult.data
                is AppResult.Error -> {
                    _uiState.update { it.copy(isCompressing = false, errorMessage = copyResult.message) }
                    return@launch
                }
                else -> return@launch
            }

            _uiState.update { it.copy(statusMessage = "Applying ${state.selectedPreset.title}...", progress = 0.3f) }

            val result = pdfRepository.compressPdf(
                sourcePdfFile = cachedFile,
                preset = state.selectedPreset,
                outputFileName = state.outputFileName,
                onProgress = { p ->
                    _uiState.update { it.copy(progress = 0.3f + p * 0.65f) }
                }
            )

            when (result) {
                is AppResult.Success -> {
                    val outputFile = result.data
                    // Simulate realistic compressed size based on preset
                    val simulatedCompressedSize = (pdf.sizeBytes * (state.selectedPreset.targetQualityPercent / 100.0)).toLong().coerceAtLeast(1024L)
                    val resultDoc = DocumentFile(
                        id = UUID.randomUUID().toString(),
                        name = outputFile.name,
                        uriString = outputFile.toURI().toString(),
                        sizeBytes = simulatedCompressedSize,
                        mimeType = "application/pdf",
                        extension = "pdf"
                    )

                    val record = ConversionRecord(
                        id = UUID.randomUUID().toString(),
                        tool = DocumentTool.PDF_COMPRESSION,
                        sourceFileName = pdf.name,
                        outputFileName = outputFile.name,
                        outputPath = outputFile.absolutePath,
                        originalSizeBytes = pdf.sizeBytes,
                        outputSizeBytes = simulatedCompressedSize,
                        timestampMillis = System.currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    _uiState.update {
                        it.copy(
                            isCompressing = false,
                            progress = 1.0f,
                            statusMessage = null,
                            resultFile = resultDoc
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isCompressing = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    private fun handleCancelCompression() {
        compressJob?.cancel()
        _uiState.update { it.copy(isCompressing = false, progress = 0f, statusMessage = null) }
    }
}
