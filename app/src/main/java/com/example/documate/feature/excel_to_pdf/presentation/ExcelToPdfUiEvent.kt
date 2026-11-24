package com.example.documate.feature.excel_to_pdf.presentation

sealed interface ExcelToPdfUiEvent {
    data class SelectFile(val uriString: String) : ExcelToPdfUiEvent
    data object RemoveFile : ExcelToPdfUiEvent
    data class SetConsent(val consented: Boolean) : ExcelToPdfUiEvent
    data class ToggleFitToWidth(val fit: Boolean) : ExcelToPdfUiEvent
    data class UpdateOutputFileName(val name: String) : ExcelToPdfUiEvent
    data object StartConversion : ExcelToPdfUiEvent
    data object CancelConversion : ExcelToPdfUiEvent
    data class SavePdfToUri(val destinationUriString: String) : ExcelToPdfUiEvent
    data object ClearError : ExcelToPdfUiEvent
    data object ClearSaveSuccess : ExcelToPdfUiEvent
    data object Reset : ExcelToPdfUiEvent
}
