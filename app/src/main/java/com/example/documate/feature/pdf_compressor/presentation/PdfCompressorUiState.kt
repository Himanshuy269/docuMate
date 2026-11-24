package com.example.documate.feature.pdf_compressor.presentation

import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.DocumentFile

data class PdfCompressorUiState(
    val selectedPdf: DocumentFile? = null,
    val selectedPreset: CompressionPreset = CompressionPreset.MEDIUM,
    val outputFileName: String = "",
    val isCompressing: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val resultFile: DocumentFile? = null,
    val errorMessage: String? = null
) {
    val canCompress: Boolean
        get() = selectedPdf != null && outputFileName.isNotBlank() && !isCompressing
}
