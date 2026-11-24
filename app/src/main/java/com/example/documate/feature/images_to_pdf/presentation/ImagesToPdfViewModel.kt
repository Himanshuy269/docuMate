package com.example.documate.feature.images_to_pdf.presentation

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

class ImagesToPdfViewModel(
    private val documentRepository: DocumentRepository,
    private val pdfRepository: PdfRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagesToPdfUiState())
    val uiState: StateFlow<ImagesToPdfUiState> = _uiState.asStateFlow()

    private var conversionJob: Job? = null

    fun onEvent(event: ImagesToPdfUiEvent) {
        when (event) {
            is ImagesToPdfUiEvent.AddImages -> handleAddImages(event.uriStrings)
            is ImagesToPdfUiEvent.RemoveImage -> handleRemoveImage(event.index)
            is ImagesToPdfUiEvent.MoveImage -> handleMoveImage(event.fromIndex, event.toIndex)
            is ImagesToPdfUiEvent.SelectPageSize -> _uiState.update { it.copy(pageSize = event.size) }
            is ImagesToPdfUiEvent.SelectOrientation -> _uiState.update { it.copy(orientation = event.orientation) }
            is ImagesToPdfUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is ImagesToPdfUiEvent.StartConversion -> handleStartConversion()
            is ImagesToPdfUiEvent.CancelConversion -> handleCancelConversion()
            is ImagesToPdfUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
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

            // Copy to app cache
            val tempFiles = mutableListOf<File>()
            for (image in state.selectedImages) {
                when (val copyResult = documentRepository.copyToAppCache(image.uriString, image.name)) {
                    is AppResult.Success -> tempFiles.add(copyResult.data)
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = "Failed to cache image ${image.name}: ${copyResult.message}"
                            )
                        }
                        return@launch
                    }
                    else -> {}
                }
            }

            _uiState.update { it.copy(statusMessage = "Generating PDF document...", progress = 0.3f) }

            val conversionResult = pdfRepository.createPdfFromImages(
                imageFiles = tempFiles,
                pageSize = state.pageSize,
                orientation = state.orientation,
                outputFileName = state.outputFileName,
                onProgress = { p ->
                    _uiState.update { it.copy(progress = 0.3f + p * 0.6f) }
                }
            )

            when (conversionResult) {
                is AppResult.Success -> {
                    val outputFile = conversionResult.data
                    val totalOriginalSize = state.selectedImages.sumOf { it.sizeBytes }
                    val resultDoc = DocumentFile(
                        id = UUID.randomUUID().toString(),
                        name = outputFile.name,
                        uriString = outputFile.toURI().toString(),
                        sizeBytes = outputFile.length(),
                        mimeType = "application/pdf",
                        extension = "pdf"
                    )

                    // Add record to history
                    val record = ConversionRecord(
                        id = UUID.randomUUID().toString(),
                        tool = DocumentTool.IMAGES_TO_PDF,
                        sourceFileName = "${state.selectedImages.size} images",
                        outputFileName = outputFile.name,
                        outputPath = outputFile.absolutePath,
                        originalSizeBytes = totalOriginalSize,
                        outputSizeBytes = outputFile.length(),
                        timestampMillis = System.currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            progress = 1.0f,
                            statusMessage = null,
                            resultFile = resultDoc
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
