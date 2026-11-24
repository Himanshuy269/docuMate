package com.example.documate.feature.word_to_pdf.presentation

import com.example.documate.core.domain.model.DocumentFile

data class WordToPdfUiState(
    val selectedFile: DocumentFile? = null,
    val hasConsentedToBackend: Boolean = false,
    val preserveHyperlinks: Boolean = true,
    val outputFileName: String = "",
    val isConverting: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val resultFile: DocumentFile? = null,
    val errorMessage: String? = null
) {
    val canConvert: Boolean
        get() = selectedFile != null && hasConsentedToBackend && outputFileName.isNotBlank() && !isConverting
}
