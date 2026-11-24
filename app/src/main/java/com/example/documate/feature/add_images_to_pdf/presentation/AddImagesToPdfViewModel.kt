package com.example.documate.feature.add_images_to_pdf.presentation

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

class AddImagesToPdfViewModel(
    private val documentRepository: DocumentRepository,
    private val pdfRepository: PdfRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddImagesToPdfUiState())
    val uiState: StateFlow<AddImagesToPdfUiState> = _uiState.asStateFlow()

    private var processJob: Job? = null

    fun onEvent(event: AddImagesToPdfUiEvent) {
        when (event) {
            is AddImagesToPdfUiEvent.SelectPdf -> handleSelectPdf(event.uriString)
            is AddImagesToPdfUiEvent.RemovePdf -> _uiState.update { it.copy(selectedPdf = null) }
            is AddImagesToPdfUiEvent.AddImages -> handleAddImages(event.uriStrings)
            is AddImagesToPdfUiEvent.RemoveImage -> handleRemoveImage(event.index)
            is AddImagesToPdfUiEvent.SelectInsertPosition -> _uiState.update { it.copy(insertPosition = event.position) }
            is AddImagesToPdfUiEvent.UpdatePageNumber -> _uiState.update { it.copy(specificPageNumber = event.page) }
            is AddImagesToPdfUiEvent.UpdateOutputFileName -> _uiState.update { it.copy(outputFileName = event.name) }
            is AddImagesToPdfUiEvent.StartProcessing -> handleStartProcessing()
            is AddImagesToPdfUiEvent.CancelProcessing -> handleCancelProcessing()
            is AddImagesToPdfUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is AddImagesToPdfUiEvent.Reset -> _uiState.update {
                AddImagesToPdfUiState(outputFileName = "Merged_${System.currentTimeMillis() / 1000}")
            }
        }
    }

    private fun handleSelectPdf(uriString: String) {
        viewModelScope.launch {
            when (val result = documentRepository.resolveFileMetadata(uriString)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPdf = result.data,
                            outputFileName = "${result.data.name.substringBeforeLast('.')}_modified"
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

    private fun handleStartProcessing() {
        val state = _uiState.value
        val pdf = state.selectedPdf ?: return
        if (!state.canProcess) return

        processJob?.cancel()
        processJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = 0.1f,
                    statusMessage = "Caching source PDF and images...",
                    errorMessage = null
                )
            }

            // Copy PDF to cache
            val cachedPdf: File = when (val pdfCopy = documentRepository.copyToAppCache(pdf.uriString, pdf.name)) {
                is AppResult.Success -> pdfCopy.data
                is AppResult.Error -> {
                    _uiState.update { it.copy(isProcessing = false, errorMessage = pdfCopy.message) }
                    return@launch
                }
                else -> return@launch
            }

            // Copy images to cache
            val cachedImages = mutableListOf<File>()
            for (img in state.selectedImages) {
                when (val imgCopy = documentRepository.copyToAppCache(img.uriString, img.name)) {
                    is AppResult.Success -> cachedImages.add(imgCopy.data)
                    is AppResult.Error -> {
                        _uiState.update { it.copy(isProcessing = false, errorMessage = imgCopy.message) }
                        return@launch
                    }
                    else -> return@launch
                }
            }

            _uiState.update { it.copy(statusMessage = "Inserting images into PDF...", progress = 0.35f) }

            val insertPage = when (state.insertPosition) {
                InsertPosition.AT_BEGINNING -> 1
                InsertPosition.AT_END -> -1
                InsertPosition.SPECIFIC_PAGE -> state.specificPageNumber
            }

            val result = pdfRepository.addImagesToPdf(
                existingPdfFile = cachedPdf,
                imageFiles = cachedImages,
                insertPositionPage = insertPage,
                outputFileName = state.outputFileName,
                onProgress = { p ->
                    _uiState.update { it.copy(progress = 0.35f + p * 0.55f) }
                }
            )

            when (result) {
                is AppResult.Success -> {
                    val outputFile = result.data
                    val totalOriginalSize = pdf.sizeBytes + state.selectedImages.sumOf { it.sizeBytes }
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
                        tool = DocumentTool.ADD_IMAGES_TO_PDF,
                        sourceFileName = "${pdf.name} + ${state.selectedImages.size} img",
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
                        it.copy(isProcessing = false, errorMessage = result.message)
                    }
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
