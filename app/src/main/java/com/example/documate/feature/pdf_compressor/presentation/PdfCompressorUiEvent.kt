package com.example.documate.feature.pdf_compressor.presentation

import com.example.documate.core.domain.model.CompressionPreset

sealed interface PdfCompressorUiEvent {
    data class SelectPdf(val uriString: String) : PdfCompressorUiEvent
    data object RemovePdf : PdfCompressorUiEvent
    data class SelectPreset(val preset: CompressionPreset) : PdfCompressorUiEvent
    data class UpdateOutputFileName(val name: String) : PdfCompressorUiEvent
    data object StartCompression : PdfCompressorUiEvent
    data object CancelCompression : PdfCompressorUiEvent
    data object ClearError : PdfCompressorUiEvent
    data object Reset : PdfCompressorUiEvent
}
