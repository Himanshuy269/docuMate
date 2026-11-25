package com.example.documate.feature.images_to_pdf.presentation

import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize

sealed interface ImagesToPdfUiEvent {
    data class AddImages(val uriStrings: List<String>) : ImagesToPdfUiEvent
    data class RemoveImage(val index: Int) : ImagesToPdfUiEvent
    data class MoveImage(val fromIndex: Int, val toIndex: Int) : ImagesToPdfUiEvent
    data object ClearAllImages : ImagesToPdfUiEvent
    data class SelectPageSize(val size: PdfPageSize) : ImagesToPdfUiEvent
    data class SelectOrientation(val orientation: PdfOrientation) : ImagesToPdfUiEvent
    data class SelectMargin(val margin: PdfMargin) : ImagesToPdfUiEvent
    data class UpdateOutputFileName(val name: String) : ImagesToPdfUiEvent
    data object StartConversion : ImagesToPdfUiEvent
    data object CancelConversion : ImagesToPdfUiEvent
    data class SavePdfToUri(val destinationUriString: String) : ImagesToPdfUiEvent
    data object ClearError : ImagesToPdfUiEvent
    data object ClearSaveSuccess : ImagesToPdfUiEvent
    data object Reset : ImagesToPdfUiEvent
}
