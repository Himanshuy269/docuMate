package com.example.documate.feature.pdf_compressor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.pdf_compressor.domain.usecase.CompressPdfUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

class PdfCompressorViewModel(
    private val documentRepository: DocumentRepository,
    private val pdfRepository: PdfRepository,
    private val compressPdfUseCase: CompressPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfCompressorUiState())
    val uiState: StateFlow<PdfCompressorUiState> = _uiState.asStateFlow()

    private var compressJob: Job? = null

    fun onEvent(event: PdfCompressorUiEvent) {
        when (event) {
            is PdfCompressorUiEvent.SelectPdf -> handleSelectPdf(event.uriString)
            is PdfCompressorUiEvent.RemovePdf -> _uiState.update { it.copy(selectedPdf = null, pdfPageCount = 0) }
            is PdfCompressorUiEvent.SelectPreset -> _uiState.update { it.copy(selectedPreset = event.preset) }
            is PdfCompressorUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is PdfCompressorUiEvent.StartCompression -> handleStartCompression()
            is PdfCompressorUiEvent.CancelCompression -> handleCancelCompression()
            is PdfCompressorUiEvent.SavePdfToUri -> handleSavePdfToUri(event.destinationUriString)
            is PdfCompressorUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is PdfCompressorUiEvent.ClearSaveSuccess -> _uiState.update { it.copy(saveSuccessMessage = null) }
            is PdfCompressorUiEvent.Reset -> _uiState.update { PdfCompressorUiState() }
        }
    }

    private fun handleSelectPdf(uriString: String) {
        viewModelScope.launch {
            when (val result = documentRepository.resolveFileMetadata(uriString)) {
                is AppResult.Success -> {
                    val pdf = result.data
                    val cleanBaseName = pdf.name.substringBeforeLast('.')

                    var pageCount = 1
                    when (val copyResult = documentRepository.copyToAppCache(pdf.uriString, pdf.name)) {
                        is AppResult.Success -> {
                            val tempFile = copyResult.data
                            when (val countResult = pdfRepository.getPdfPageCount(tempFile)) {
                                is AppResult.Success -> pageCount = countResult.data.coerceAtLeast(1)
                                else -> {}
                            }
                            try { tempFile.delete() } catch (_: Exception) {}
                        }
                        else -> {}
                    }

                    _uiState.update {
                        it.copy(
                            selectedPdf = pdf,
                            pdfPageCount = pageCount,
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
        if (!state.canCompress) return

        compressJob?.cancel()
        compressJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCompressing = true,
                    progress = 0.05f,
                    statusMessage = "Analyzing PDF streams & preparing compression...",
                    errorMessage = null
                )
            }

            val result = compressPdfUseCase(
                sourcePdf = state.selectedPdf,
                preset = state.selectedPreset,
                outputFileName = state.outputFileName,
                onProgress = { p, status ->
                    _uiState.update { it.copy(progress = p, statusMessage = status) }
                }
            )

            when (result) {
                is AppResult.Success -> {
                    val resultDoc = result.data
                    val shareableUri = try {
                        val file = File(URI(resultDoc.uriString))
                        when (val shareResult = documentRepository.getShareableUri(file)) {
                            is AppResult.Success -> shareResult.data
                            else -> resultDoc.uriString
                        }
                    } catch (_: Exception) {
                        resultDoc.uriString
                    }

                    _uiState.update {
                        it.copy(
                            isCompressing = false,
                            progress = 1.0f,
                            statusMessage = null,
                            resultFile = resultDoc,
                            shareableUriString = shareableUri
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

    private fun handleSavePdfToUri(destinationUriString: String) {
        val resultFile = _uiState.value.resultFile ?: return
        viewModelScope.launch {
            try {
                val file = File(URI(resultFile.uriString))
                when (val saveResult = documentRepository.copyFileToUri(file, destinationUriString)) {
                    is AppResult.Success -> {
                        _uiState.update { it.copy(saveSuccessMessage = "Compressed PDF saved successfully!") }
                    }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(errorMessage = "Failed to save compressed PDF: ${saveResult.message}") }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save file: ${e.message}") }
            }
        }
    }

    private fun handleCancelCompression() {
        compressJob?.cancel()
        _uiState.update { it.copy(isCompressing = false, progress = 0f, statusMessage = null) }
    }
}
