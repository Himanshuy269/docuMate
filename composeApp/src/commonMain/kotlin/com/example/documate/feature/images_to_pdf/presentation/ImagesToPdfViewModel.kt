package com.example.documate.feature.images_to_pdf.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.common.currentTimeMillis
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.feature.images_to_pdf.domain.usecase.ConvertImagesToPdfUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImagesToPdfViewModel(
    private val documentRepository: DocumentRepository,
    private val convertImagesToPdfUseCase: ConvertImagesToPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagesToPdfUiState())
    val uiState: StateFlow<ImagesToPdfUiState> = _uiState.asStateFlow()

    private var conversionJob: Job? = null

    fun onEvent(event: ImagesToPdfUiEvent) {
        when (event) {
            is ImagesToPdfUiEvent.AddImages -> handleAddImages(event.uriStrings)
            is ImagesToPdfUiEvent.RemoveImage -> handleRemoveImage(event.index)
            is ImagesToPdfUiEvent.MoveImage -> handleMoveImage(event.fromIndex, event.toIndex)
            is ImagesToPdfUiEvent.ClearAllImages -> _uiState.update { it.copy(selectedImages = emptyList()) }
            is ImagesToPdfUiEvent.SelectPageSize -> _uiState.update { it.copy(pageSize = event.size) }
            is ImagesToPdfUiEvent.SelectOrientation -> _uiState.update { it.copy(orientation = event.orientation) }
            is ImagesToPdfUiEvent.SelectMargin -> _uiState.update { it.copy(margin = event.margin) }
            is ImagesToPdfUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is ImagesToPdfUiEvent.StartConversion -> handleStartConversion()
            is ImagesToPdfUiEvent.CancelConversion -> handleCancelConversion()
            is ImagesToPdfUiEvent.SavePdfToUri -> handleSavePdfToUri(event.destinationUriString)
            is ImagesToPdfUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is ImagesToPdfUiEvent.ClearSaveSuccess -> _uiState.update { it.copy(saveSuccessMessage = null) }
            is ImagesToPdfUiEvent.Reset -> _uiState.update {
                ImagesToPdfUiState(outputFileName = "Images_${currentTimeMillis() / 1000}")
            }
        }
    }

    private fun handleAddImages(uriStrings: List<String>) {
        viewModelScope.launch {
            val resolvedList = mutableListOf<DocumentFile>()
            for (uri in uriStrings) {
                when (val result = documentRepository.resolveFileMetadata(uri)) {
                    is AppResult.Success -> resolvedList.add(result.data)
                    is AppResult.Error -> {
                        _uiState.update { it.copy(errorMessage = result.message) }
                    }
                    else -> {}
                }
            }
            _uiState.update { current ->
                val updated = current.selectedImages + resolvedList
                current.copy(selectedImages = updated)
            }
        }
    }

    private fun handleRemoveImage(index: Int) {
        _uiState.update { current ->
            if (index in current.selectedImages.indices) {
                val updated = current.selectedImages.toMutableList().apply { removeAt(index) }
                current.copy(selectedImages = updated)
            } else {
                current
            }
        }
    }

    private fun handleMoveImage(fromIndex: Int, toIndex: Int) {
        _uiState.update { current ->
            val list = current.selectedImages.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                current.copy(selectedImages = list)
            } else {
                current
            }
        }
    }

    private fun handleStartConversion() {
        val currentState = _uiState.value
        if (!currentState.canConvert) return

        conversionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = 0f,
                    statusMessage = "Starting conversion...",
                    errorMessage = null,
                    resultFile = null,
                    shareableUriString = null
                )
            }

            val conversionResult = convertImagesToPdfUseCase(
                selectedImages = currentState.selectedImages,
                pageSize = currentState.pageSize,
                orientation = currentState.orientation,
                margin = currentState.margin,
                outputFileName = currentState.outputFileName,
                onProgress = { p, status ->
                    _uiState.update { it.copy(progress = p, statusMessage = status) }
                }
            )

            when (conversionResult) {
                is AppResult.Success -> {
                    val resultDoc = conversionResult.data
                    val platformFile = PlatformFile(path = resultDoc.uriString, name = resultDoc.name, sizeBytes = resultDoc.sizeBytes)
                    val shareableUri = when (val shareResult = documentRepository.getShareableUri(platformFile)) {
                        is AppResult.Success -> shareResult.data
                        else -> resultDoc.uriString
                    }

                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            progress = 1.0f,
                            statusMessage = null,
                            resultFile = resultDoc,
                            shareableUriString = shareableUri
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = conversionResult.message
                        )
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
                    _uiState.update { it.copy(saveSuccessMessage = "PDF saved to selected destination successfully!") }
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
        _uiState.update {
            it.copy(
                isProcessing = false,
                progress = 0f,
                statusMessage = null
            )
        }
    }
}
