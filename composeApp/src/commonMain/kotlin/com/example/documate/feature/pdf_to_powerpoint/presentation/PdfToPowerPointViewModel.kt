package com.example.documate.feature.pdf_to_powerpoint.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.di.AppContainerHolder
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.feature.pdf_to_powerpoint.domain.usecase.ConvertPdfToPowerPointUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfToPowerPointViewModel(
    private val documentRepository: DocumentRepository,
    private val convertPdfToPowerPointUseCase: ConvertPdfToPowerPointUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfToPowerPointUiState())
    val uiState: StateFlow<PdfToPowerPointUiState> = _uiState.asStateFlow()

    private var conversionJob: Job? = null

    fun onEvent(event: PdfToPowerPointUiEvent) {
        when (event) {
            is PdfToPowerPointUiEvent.SelectFile -> handleSelectFile(event.uriString)
            is PdfToPowerPointUiEvent.RemoveFile -> _uiState.update { it.copy(selectedFile = null, outputFileName = "") }
            is PdfToPowerPointUiEvent.SetConsent -> _uiState.update { it.copy(hasConsentedToBackend = event.consented) }
            is PdfToPowerPointUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is PdfToPowerPointUiEvent.StartConversion -> handleStartConversion()
            is PdfToPowerPointUiEvent.CancelConversion -> handleCancelConversion()
            is PdfToPowerPointUiEvent.SaveFileToUri -> handleSaveFileToUri(event.destinationUriString)
            is PdfToPowerPointUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is PdfToPowerPointUiEvent.ClearSaveSuccess -> _uiState.update { it.copy(saveSuccessMessage = null) }
            is PdfToPowerPointUiEvent.Reset -> _uiState.update { PdfToPowerPointUiState() }
        }
    }

    private fun handleSelectFile(uriString: String) {
        viewModelScope.launch {
            when (val result = documentRepository.resolveFileMetadata(uriString)) {
                is AppResult.Success -> {
                    val file = result.data
                    val ext = file.extension.lowercase()
                    if (ext != "pdf") {
                        _uiState.update {
                            it.copy(errorMessage = "Unsupported file type ($ext). Please select a PDF document (.pdf).")
                        }
                        return@launch
                    }
                    val cleanBaseName = file.name.substringBeforeLast('.')
                    _uiState.update {
                        it.copy(
                            selectedFile = file,
                            outputFileName = "${cleanBaseName}_slides"
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
        if (!state.canConvert) return

        conversionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    progress = 0f,
                    statusMessage = "Preparing PDF document...",
                    errorMessage = null
                )
            }

            val backendUrl = if (AppContainerHolder.isInitialized) {
                AppContainerHolder.current.settingsRepository.getSettingsSync().backendServiceUrl
            } else {
                "http://10.0.2.2:8080"
            }

            val result = convertPdfToPowerPointUseCase(
                sourceFile = state.selectedFile,
                hasUserConsented = state.hasConsentedToBackend,
                backendBaseUrl = backendUrl,
                customOutputName = state.outputFileName,
                onProgress = { p, status ->
                    _uiState.update { it.copy(progress = p, statusMessage = status) }
                }
            )

            when (result) {
                is AppResult.Success -> {
                    val resultDoc = result.data
                    val platformFile = PlatformFile(path = resultDoc.uriString, name = resultDoc.name, sizeBytes = resultDoc.sizeBytes)
                    val shareableUri = when (val shareResult = documentRepository.getShareableUri(platformFile)) {
                        is AppResult.Success -> shareResult.data
                        else -> resultDoc.uriString
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

    private fun handleSaveFileToUri(destinationUriString: String) {
        val resultFile = _uiState.value.resultFile ?: return
        viewModelScope.launch {
            val platformFile = PlatformFile(path = resultFile.uriString, name = resultFile.name, sizeBytes = resultFile.sizeBytes)
            when (val saveResult = documentRepository.copyFileToUri(platformFile, destinationUriString)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(saveSuccessMessage = "PowerPoint presentation saved successfully!") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to save presentation: ${saveResult.message}") }
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
