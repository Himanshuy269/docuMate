package com.example.documate.feature.powerpoint_to_pdf.presentation

sealed interface PowerPointToPdfUiEvent {
    data class SelectFile(val uriString: String) : PowerPointToPdfUiEvent
    data object RemoveFile : PowerPointToPdfUiEvent
    data class SetConsent(val consented: Boolean) : PowerPointToPdfUiEvent
    data class SelectSlidesPerPage(val count: Int) : PowerPointToPdfUiEvent
    data class UpdateOutputFileName(val name: String) : PowerPointToPdfUiEvent
    data object StartConversion : PowerPointToPdfUiEvent
    data object CancelConversion : PowerPointToPdfUiEvent
    data object ClearError : PowerPointToPdfUiEvent
    data object Reset : PowerPointToPdfUiEvent
}
