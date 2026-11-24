package com.example.documate.feature.images_to_pdf.presentation

import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize

data class ImagesToPdfUiState(
    val selectedImages: List<DocumentFile> = emptyList(),
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val orientation: PdfOrientation = PdfOrientation.PORTRAIT,
    val outputFileName: String = "Images_${System.currentTimeMillis() / 1000}",
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val resultFile: DocumentFile? = null,
    val errorMessage: String? = null
) {
    val canConvert: Boolean
        get() = selectedImages.isNotEmpty() && outputFileName.isNotBlank() && !isProcessing
}
