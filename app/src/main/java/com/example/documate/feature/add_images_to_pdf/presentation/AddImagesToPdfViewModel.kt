package com.example.documate.feature.add_images_to_pdf.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.add_images_to_pdf.domain.usecase.AddImagesToPdfUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

class AddImagesToPdfViewModel(
    private val documentRepository: DocumentRepository,
    private val pdfRepository: PdfRepository,
    private val addImagesToPdfUseCase: AddImagesToPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddImagesToPdfUiState())
    val uiState: StateFlow<AddImagesToPdfUiState> = _uiState.asStateFlow()

    private var processJob: Job? = null

    fun onEvent(event: AddImagesToPdfUiEvent) {
        when (event) {
            is AddImagesToPdfUiEvent.SelectPdf -> handleSelectPdf(event.uriString)
            is AddImagesToPdfUiEvent.RemovePdf -> _uiState.update { it.copy(selectedPdf = null, pdfPageCount = 0) }
            is AddImagesToPdfUiEvent.AddImages -> handleAddImages(event.uriStrings)
            is AddImagesToPdfUiEvent.RemoveImage -> handleRemoveImage(event.index)
            is AddImagesToPdfUiEvent.MoveImage -> handleMoveImage(event.fromIndex, event.toIndex)
            is AddImagesToPdfUiEvent.ClearAllImages -> _uiState.update { it.copy(selectedImages = emptyList()) }
            is AddImagesToPdfUiEvent.SelectInsertPosition -> _uiState.update { it.copy(insertPosition = event.position) }
            is AddImagesToPdfUiEvent.UpdatePageNumber -> _uiState.update { it.copy(specificPageNumber = event.page) }
            is AddImagesToPdfUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is AddImagesToPdfUiEvent.StartProcessing -> handleStartProcessing()
            is AddImagesToPdfUiEvent.CancelProcessing -> handleCancelProcessing()
            is AddImagesToPdfUiEvent.SavePdfToUri -> handleSavePdfToUri(event.destinationUriString)
            is AddImagesToPdfUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is AddImagesToPdfUiEvent.ClearSaveSuccess -> _uiState.update { it.copy(saveSuccessMessage = null) }
            is AddImagesToPdfUiEvent.Reset -> _uiState.update {
                AddImagesToPdfUiState(outputFileName = "Merged_${System.currentTimeMillis() / 1000}")
            }
        }
    }

    private fun handleSelectPdf(uriString: String) {
        viewModelScope.launch {
            when (val result = documentRepository.resolveFileMetadata(uriString)) {
                is AppResult.Success -> {
                    val pdfDoc = result.data
                    val cleanBase = pdfDoc.name.substringBeforeLast('.')

                    // Inspect page count from temporary cache
                    var pageCount = 1
                    when (val copyResult = documentRepository.copyToAppCache(pdfDoc.uriString, pdfDoc.name)) {
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
                            selectedPdf = pdfDoc,
                            pdfPageCount = pageCount,
                            specificPageNumber = pageCount,
                            outputFileName = "${cleanBase}_modified"
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

    private fun handleStartProcessing() {
        val state = _uiState.value
        if (!state.canProcess) return

        processJob?.cancel()
        processJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = 0.05f,
                    statusMessage = "Preparing source PDF and images...",
                    errorMessage = null
                )
            }

            val result = addImagesToPdfUseCase(
                sourcePdf = state.selectedPdf,
                selectedImages = state.selectedImages,
                insertPosition = state.insertPosition,
                specificPageNumber = state.specificPageNumber,
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
                        it.copy(isProcessing = false, errorMessage = result.message)
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
                        _uiState.update { it.copy(saveSuccessMessage = "Modified PDF saved successfully!") }
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

    private fun handleCancelProcessing() {
        processJob?.cancel()
        _uiState.update { it.copy(isProcessing = false, progress = 0f, statusMessage = null) }
    }
}
