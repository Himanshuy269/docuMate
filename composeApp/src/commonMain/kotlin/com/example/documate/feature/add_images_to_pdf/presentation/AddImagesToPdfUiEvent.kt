package com.example.documate.feature.add_images_to_pdf.presentation

sealed interface AddImagesToPdfUiEvent {
    data class SelectPdf(val uriString: String) : AddImagesToPdfUiEvent
    data object RemovePdf : AddImagesToPdfUiEvent
    data class AddImages(val uriStrings: List<String>) : AddImagesToPdfUiEvent
    data class RemoveImage(val index: Int) : AddImagesToPdfUiEvent
    data class MoveImage(val fromIndex: Int, val toIndex: Int) : AddImagesToPdfUiEvent
    data object ClearAllImages : AddImagesToPdfUiEvent
    data class SelectInsertPosition(val position: InsertPosition) : AddImagesToPdfUiEvent
    data class UpdatePageNumber(val page: Int) : AddImagesToPdfUiEvent
    data class UpdateOutputFileName(val name: String) : AddImagesToPdfUiEvent
    data object StartProcessing : AddImagesToPdfUiEvent
    data object CancelProcessing : AddImagesToPdfUiEvent
    data class SavePdfToUri(val destinationUriString: String) : AddImagesToPdfUiEvent
    data object ClearError : AddImagesToPdfUiEvent
    data object ClearSaveSuccess : AddImagesToPdfUiEvent
    data object Reset : AddImagesToPdfUiEvent
}
