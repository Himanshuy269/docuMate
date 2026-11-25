package com.example.documate.feature.pdf_to_powerpoint.presentation

sealed interface PdfToPowerPointUiEvent {
    data class SelectFile(val uriString: String) : PdfToPowerPointUiEvent
    data object RemoveFile : PdfToPowerPointUiEvent
    data class SetConsent(val consented: Boolean) : PdfToPowerPointUiEvent
    data class UpdateOutputFileName(val name: String) : PdfToPowerPointUiEvent
    data object StartConversion : PdfToPowerPointUiEvent
    data object CancelConversion : PdfToPowerPointUiEvent
    data class SaveFileToUri(val destinationUriString: String) : PdfToPowerPointUiEvent
    data object ClearError : PdfToPowerPointUiEvent
    data object ClearSaveSuccess : PdfToPowerPointUiEvent
    data object Reset : PdfToPowerPointUiEvent
}
