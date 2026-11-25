package com.example.documate.feature.pdf_to_excel.presentation

sealed interface PdfToExcelUiEvent {
    data class SelectFile(val uriString: String) : PdfToExcelUiEvent
    data object RemoveFile : PdfToExcelUiEvent
    data class SetConsent(val consented: Boolean) : PdfToExcelUiEvent
    data class UpdateOutputFileName(val name: String) : PdfToExcelUiEvent
    data object StartConversion : PdfToExcelUiEvent
    data object CancelConversion : PdfToExcelUiEvent
    data class SaveFileToUri(val destinationUriString: String) : PdfToExcelUiEvent
    data object ClearError : PdfToExcelUiEvent
    data object ClearSaveSuccess : PdfToExcelUiEvent
    data object Reset : PdfToExcelUiEvent
}
