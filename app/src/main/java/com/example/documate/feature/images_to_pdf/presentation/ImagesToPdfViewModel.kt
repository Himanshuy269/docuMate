package com.example.documate.feature.images_to_pdf.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.feature.images_to_pdf.domain.usecase.ConvertImagesToPdfUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

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
                ImagesToPdfUiState(outputFileName = "Images_${System.currentTimeMillis() / 1000}")
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
            if (resolvedList.isNotEmpty()) {
                _uiState.update { current ->
                    current.copy(selectedImages = current.selectedImages + resolvedList)
                }
            }
        }
    }

    private fun handleRemoveImage(index: Int) {
        _uiState.update { current ->
            if (index in current.selectedImages.indices) {
                val updated = current.selectedImages.toMutableList().apply { removeAt(index) }
                current.copy(selectedImages = updated)
            } else current
        }
    }

    private fun handleMoveImage(fromIndex: Int, toIndex: Int) {
        _uiState.update { current ->
            if (fromIndex in current.selectedImages.indices && toIndex in current.selectedImages.indices) {
                val updated = current.selectedImages.toMutableList()
                val item = updated.removeAt(fromIndex)
                updated.add(toIndex, item)
                current.copy(selectedImages = updated)
            } else current
        }
    }

    private fun handleStartConversion() {
        val state = _uiState.value
        if (!state.canConvert) return

        conversionJob?.cancel()
        conversionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = 0.05f,
                    statusMessage = "Preparing images for conversion...",
                    errorMessage = null
                )
            }

            val conversionResult = convertImagesToPdfUseCase(
                selectedImages = state.selectedImages,
                pageSize = state.pageSize,
                orientation = state.orientation,
                margin = state.margin,
                outputFileName = state.outputFileName,
                onProgress = { p, status ->
                    _uiState.update { it.copy(progress = p, statusMessage = status) }
                }
            )

            when (conversionResult) {
                is AppResult.Success -> {
                    val resultDoc = conversionResult.data
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
            try {
                val file = File(URI(resultFile.uriString))
                when (val saveResult = documentRepository.copyFileToUri(file, destinationUriString)) {
                    is AppResult.Success -> {
                        _uiState.update { it.copy(saveSuccessMessage = "PDF saved to selected destination successfully!") }
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
        _uiState.update {
            it.copy(
                isProcessing = false,
                progress = 0f,
                statusMessage = null
            )
        }
    }
}
