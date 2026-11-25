package com.example.documate.feature.add_images_to_pdf.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.common.currentTimeMillis
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.add_images_to_pdf.domain.usecase.AddImagesToPdfUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                AddImagesToPdfUiState(outputFileName = "Merged_${currentTimeMillis() / 1000}")
            }
        }
    }

    private fun handleSelectPdf(uriString: String) {
        viewModelScope.launch {
            when (val result = documentRepository.resolveFileMetadata(uriString)) {
                is AppResult.Success -> {
                    val pdfDoc = result.data
                    val cleanBase = pdfDoc.name.substringBeforeLast('.')

                    var pageCount = 1
                    when (val copyResult = documentRepository.copyToAppCache(pdfDoc.uriString, pdfDoc.name)) {
                        is AppResult.Success -> {
                            val tempFile = copyResult.data
                            when (val countResult = pdfRepository.getPdfPageCount(tempFile)) {
                                is AppResult.Success -> pageCount = countResult.data.coerceAtLeast(1)
                                else -> {}
                            }
                            documentRepository.clearTemporaryFiles()
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
                    is AppResult.Error -> _uiState.update { it.copy(errorMessage = result.message) }
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
                val list = current.selectedImages.toMutableList().apply { removeAt(index) }
                current.copy(selectedImages = list)
            } else current
        }
    }

    private fun handleMoveImage(fromIndex: Int, toIndex: Int) {
        _uiState.update { current ->
            val list = current.selectedImages.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                current.copy(selectedImages = list)
            } else current
        }
    }

    private fun handleStartProcessing() {
        val state = _uiState.value
        if (!state.canProcess) return

        processJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = 0f,
                    statusMessage = "Preparing source PDF and images...",
                    errorMessage = null
                )
            }

            val result = addImagesToPdfUseCase(
                existingPdf = state.selectedPdf,
                newImages = state.selectedImages,
                insertPosition = state.insertPosition,
                customPageNumber = state.specificPageNumber,
                outputFileName = state.outputFileName,
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
            val platformFile = PlatformFile(path = resultFile.uriString, name = resultFile.name, sizeBytes = resultFile.sizeBytes)
            when (val saveResult = documentRepository.copyFileToUri(platformFile, destinationUriString)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(saveSuccessMessage = "Modified PDF saved successfully!") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to save PDF: ${saveResult.message}") }
                }
                else -> {}
            }
        }
    }

    private fun handleCancelProcessing() {
        processJob?.cancel()
        _uiState.update { it.copy(isProcessing = false, progress = 0f, statusMessage = null) }
    }
}
