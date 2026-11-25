package com.example.documate.feature.powerpoint_to_pdf.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.di.AppContainerHolder
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.feature.powerpoint_to_pdf.domain.usecase.ConvertPowerPointToPdfUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PowerPointToPdfViewModel(
    private val documentRepository: DocumentRepository,
    private val convertPowerPointToPdfUseCase: ConvertPowerPointToPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PowerPointToPdfUiState())
    val uiState: StateFlow<PowerPointToPdfUiState> = _uiState.asStateFlow()

    private var conversionJob: Job? = null

    fun onEvent(event: PowerPointToPdfUiEvent) {
        when (event) {
            is PowerPointToPdfUiEvent.SelectFile -> handleSelectFile(event.uriString)
            is PowerPointToPdfUiEvent.RemoveFile -> _uiState.update { it.copy(selectedFile = null, outputFileName = "") }
            is PowerPointToPdfUiEvent.SetConsent -> _uiState.update { it.copy(hasConsentedToBackend = event.consented) }
            is PowerPointToPdfUiEvent.SelectSlidesPerPage -> _uiState.update { it.copy(slidesPerPage = event.count) }
            is PowerPointToPdfUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is PowerPointToPdfUiEvent.StartConversion -> handleStartConversion()
            is PowerPointToPdfUiEvent.CancelConversion -> handleCancelConversion()
            is PowerPointToPdfUiEvent.SavePdfToUri -> handleSavePdfToUri(event.destinationUriString)
            is PowerPointToPdfUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is PowerPointToPdfUiEvent.ClearSaveSuccess -> _uiState.update { it.copy(saveSuccessMessage = null) }
            is PowerPointToPdfUiEvent.Reset -> _uiState.update { PowerPointToPdfUiState() }
        }
    }

    private fun handleSelectFile(uriString: String) {
        viewModelScope.launch {
            when (val result = documentRepository.resolveFileMetadata(uriString)) {
                is AppResult.Success -> {
                    val file = result.data
                    val ext = file.extension.lowercase()
                    if (ext != "pptx" && ext != "ppt") {
                        _uiState.update {
                            it.copy(errorMessage = "Unsupported file type ($ext). Please select a PowerPoint presentation (.pptx or .ppt).")
                        }
                        return@launch
                    }
                    val cleanBaseName = file.name.substringBeforeLast('.')
                    _uiState.update {
                        it.copy(
                            selectedFile = file,
                            outputFileName = "${cleanBaseName}_presentation"
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
                    statusMessage = "Preparing PowerPoint presentation...",
                    errorMessage = null
                )
            }

            val backendUrl = if (AppContainerHolder.isInitialized) {
                AppContainerHolder.current.settingsRepository.getSettingsSync().backendServiceUrl
            } else {
                "http://10.0.2.2:8080"
            }

            val result = convertPowerPointToPdfUseCase(
                sourceFile = state.selectedFile,
                hasUserConsented = state.hasConsentedToBackend,
                backendBaseUrl = backendUrl,
                slidesPerPage = state.slidesPerPage,
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

    private fun handleSavePdfToUri(destinationUriString: String) {
        val resultFile = _uiState.value.resultFile ?: return
        viewModelScope.launch {
            val platformFile = PlatformFile(path = resultFile.uriString, name = resultFile.name, sizeBytes = resultFile.sizeBytes)
            when (val saveResult = documentRepository.copyFileToUri(platformFile, destinationUriString)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(saveSuccessMessage = "PowerPoint presentation saved as PDF successfully!") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to save PDF: ${saveResult.message}") }
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
