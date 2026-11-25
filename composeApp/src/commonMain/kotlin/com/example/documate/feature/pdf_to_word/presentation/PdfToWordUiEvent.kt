package com.example.documate.feature.pdf_to_word.presentation

sealed interface PdfToWordUiEvent {
    data class SelectFile(val uriString: String) : PdfToWordUiEvent
    data object RemoveFile : PdfToWordUiEvent
    data class SetConsent(val consented: Boolean) : PdfToWordUiEvent
    data class UpdateOutputFileName(val name: String) : PdfToWordUiEvent
    data object StartConversion : PdfToWordUiEvent
    data object CancelConversion : PdfToWordUiEvent
    data class SaveFileToUri(val destinationUriString: String) : PdfToWordUiEvent
    data object ClearError : PdfToWordUiEvent
    data object ClearSaveSuccess : PdfToWordUiEvent
    data object Reset : PdfToWordUiEvent
}
