package com.example.documate.feature.pdf_to_powerpoint.presentation

import com.example.documate.core.domain.model.DocumentFile

data class PdfToPowerPointUiState(
    val selectedFile: DocumentFile? = null,
    val hasConsentedToBackend: Boolean = false,
    val outputFileName: String = "",
    val isConverting: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val resultFile: DocumentFile? = null,
    val shareableUriString: String? = null,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null
) {
    val canConvert: Boolean
        get() = selectedFile != null && hasConsentedToBackend && outputFileName.isNotBlank() && !isConverting
}
