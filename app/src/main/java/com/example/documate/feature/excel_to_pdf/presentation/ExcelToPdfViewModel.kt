package com.example.documate.feature.excel_to_pdf.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.feature.excel_to_pdf.domain.usecase.ConvertExcelToPdfUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

class ExcelToPdfViewModel(
    private val documentRepository: DocumentRepository,
    private val convertExcelToPdfUseCase: ConvertExcelToPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExcelToPdfUiState())
    val uiState: StateFlow<ExcelToPdfUiState> = _uiState.asStateFlow()

    private var conversionJob: Job? = null

    fun onEvent(event: ExcelToPdfUiEvent) {
        when (event) {
            is ExcelToPdfUiEvent.SelectFile -> handleSelectFile(event.uriString)
            is ExcelToPdfUiEvent.RemoveFile -> _uiState.update { it.copy(selectedFile = null, outputFileName = "") }
            is ExcelToPdfUiEvent.SetConsent -> _uiState.update { it.copy(hasConsentedToBackend = event.consented) }
            is ExcelToPdfUiEvent.ToggleFitToWidth -> _uiState.update { it.copy(fitToPageWidth = event.fit) }
            is ExcelToPdfUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is ExcelToPdfUiEvent.StartConversion -> handleStartConversion()
            is ExcelToPdfUiEvent.CancelConversion -> handleCancelConversion()
            is ExcelToPdfUiEvent.SavePdfToUri -> handleSavePdfToUri(event.destinationUriString)
            is ExcelToPdfUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is ExcelToPdfUiEvent.ClearSaveSuccess -> _uiState.update { it.copy(saveSuccessMessage = null) }
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
                            outputFileName = "${cleanBaseName}_sheet"
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
                    progress = 0.05f,
                    statusMessage = "Caching Excel workbook...",
                    errorMessage = null
                )
            }

            val result = convertExcelToPdfUseCase(
                sourceFile = file,
                hasConsentedToBackend = state.hasConsentedToBackend,
                fitToPageWidth = state.fitToPageWidth,
                outputFileName = state.outputFileName,
                onProgress = { p, status ->
                    _uiState.update { it.copy(progress = p, statusMessage = status) }
                }
            )

            when (result) {
                is AppResult.Success -> {
                    val resultDoc = result.data
                    val shareableUri = try {
                        val outputFile = File(URI(resultDoc.uriString))
                        when (val shareResult = documentRepository.getShareableUri(outputFile)) {
                            is AppResult.Success -> shareResult.data
                            else -> resultDoc.uriString
                        }
                    } catch (_: Exception) {
                        resultDoc.uriString
                    }

                    _uiState.update {
                        it.copy(
                            isConverting = false,
                            progress = 1.0f,
                            statusMessage = null,
                            resultFile = resultDoc,
                            shareableUriString = shareableUri
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

    private fun handleSavePdfToUri(destinationUriString: String) {
        val resultFile = _uiState.value.resultFile ?: return
        viewModelScope.launch {
            try {
                val file = File(URI(resultFile.uriString))
                when (val saveResult = documentRepository.copyFileToUri(file, destinationUriString)) {
                    is AppResult.Success -> {
                        _uiState.update { it.copy(saveSuccessMessage = "Excel workbook saved as PDF successfully!") }
                    }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(errorMessage = "Failed to save PDF: ${saveResult.message}") }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save file: ${e.message}") }
            }
        }
    }

    private fun handleCancelConversion() {
        conversionJob?.cancel()
        _uiState.update { it.copy(isConverting = false, progress = 0f, statusMessage = null) }
    }
}
