package com.example.documate.feature.excel_to_pdf.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ExcelToPdfViewModel(
    private val documentRepository: DocumentRepository,
    private val officeConversionRepository: OfficeConversionRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExcelToPdfUiState())
    val uiState: StateFlow<ExcelToPdfUiState> = _uiState.asStateFlow()

    private var conversionJob: Job? = null

    fun onEvent(event: ExcelToPdfUiEvent) {
        when (event) {
            is ExcelToPdfUiEvent.SelectFile -> handleSelectFile(event.uriString)
            is ExcelToPdfUiEvent.RemoveFile -> _uiState.update { it.copy(selectedFile = null) }
            is ExcelToPdfUiEvent.SetConsent -> _uiState.update { it.copy(hasConsentedToBackend = event.consented) }
            is ExcelToPdfUiEvent.ToggleFitToWidth -> _uiState.update { it.copy(fitToPageWidth = event.fit) }
            is ExcelToPdfUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is ExcelToPdfUiEvent.StartConversion -> handleStartConversion()
            is ExcelToPdfUiEvent.CancelConversion -> handleCancelConversion()
            is ExcelToPdfUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is ExcelToPdfUiEvent.Reset -> _uiState.update { ExcelToPdfUiState() }
        }
    }

    private fun handleSelectFile(uriString: String) {
        viewModelScope.launch {
            when (val result = documentRepository.resolveFileMetadata(uriString)) {
                is AppResult.Success -> {
                    val file = result.data
                    val ext = file.extension.lowercase()
                    if (ext != "xlsx" && ext != "xls") {
                        _uiState.update {
                            it.copy(errorMessage = "Unsupported file type ($ext). Please select an Excel workbook (.xlsx or .xls).")
                        }
                        return@launch
                    }
                    val cleanBaseName = file.name.substringBeforeLast('.')
                    _uiState.update {
                        it.copy(
                            selectedFile = file,
                            outputFileName = "${cleanBaseName}_sheet.pdf"
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

    private fun handleStartConversion() {
        val state = _uiState.value
        val file = state.selectedFile ?: return
        if (!state.canConvert) return

        conversionJob?.cancel()
        conversionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    progress = 0.1f,
                    statusMessage = "Caching workbook...",
                    errorMessage = null
                )
            }

            val cachedFile: File = when (val copyResult = documentRepository.copyToAppCache(file.uriString, file.name)) {
                is AppResult.Success -> copyResult.data
                is AppResult.Error -> {
                    _uiState.update { it.copy(isConverting = false, errorMessage = copyResult.message) }
                    return@launch
                }
                else -> return@launch
            }

            _uiState.update { it.copy(statusMessage = "Converting Excel tables to PDF via LibreOffice...", progress = 0.3f) }

            val result = officeConversionRepository.convertOfficeDocumentToPdf(
                sourceFile = cachedFile,
                tool = DocumentTool.EXCEL_TO_PDF,
                backendBaseUrl = "http://10.0.2.2:8080",
                onProgress = { p ->
                    _uiState.update { it.copy(progress = 0.3f + p * 0.65f) }
                }
            )

            when (result) {
                is AppResult.Success -> {
                    val outputFile = result.data
                    val resultDoc = DocumentFile(
                        id = UUID.randomUUID().toString(),
                        name = outputFile.name,
                        uriString = outputFile.toURI().toString(),
                        sizeBytes = outputFile.length(),
                        mimeType = "application/pdf",
                        extension = "pdf"
                    )

                    val record = ConversionRecord(
                        id = UUID.randomUUID().toString(),
                        tool = DocumentTool.EXCEL_TO_PDF,
                        sourceFileName = file.name,
                        outputFileName = outputFile.name,
                        outputPath = outputFile.absolutePath,
                        originalSizeBytes = file.sizeBytes,
                        outputSizeBytes = outputFile.length(),
                        timestampMillis = System.currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    _uiState.update {
                        it.copy(
                            isConverting = false,
                            progress = 1.0f,
                            statusMessage = null,
                            resultFile = resultDoc
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isConverting = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    private fun handleCancelConversion() {
        conversionJob?.cancel()
        _uiState.update { it.copy(isConverting = false, progress = 0f, statusMessage = null) }
    }
}
