package com.example.documate.feature.add_images_to_pdf.presentation

import com.example.documate.core.common.currentTimeMillis
import com.example.documate.core.domain.model.DocumentFile

enum class InsertPosition(val title: String) {
    AT_BEGINNING("At the beginning"),
    AT_END("At the end"),
    SPECIFIC_PAGE("After specific page")
}

data class AddImagesToPdfUiState(
    val selectedPdf: DocumentFile? = null,
    val pdfPageCount: Int = 0,
    val selectedImages: List<DocumentFile> = emptyList(),
    val insertPosition: InsertPosition = InsertPosition.AT_END,
    val specificPageNumber: Int = 1,
    val outputFileName: String = "Merged_${currentTimeMillis() / 1000}",
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val resultFile: DocumentFile? = null,
    val shareableUriString: String? = null,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null
) {
    val canProcess: Boolean
        get() = selectedPdf != null && selectedImages.isNotEmpty() && outputFileName.isNotBlank() && !isProcessing
}
