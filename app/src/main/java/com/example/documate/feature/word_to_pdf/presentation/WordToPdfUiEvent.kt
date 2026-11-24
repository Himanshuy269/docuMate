package com.example.documate.feature.word_to_pdf.presentation

sealed interface WordToPdfUiEvent {
    data class SelectFile(val uriString: String) : WordToPdfUiEvent
    data object RemoveFile : WordToPdfUiEvent
    data class SetConsent(val consented: Boolean) : WordToPdfUiEvent
    data class TogglePreserveHyperlinks(val preserve: Boolean) : WordToPdfUiEvent
    data class UpdateOutputFileName(val name: String) : WordToPdfUiEvent
    data object StartConversion : WordToPdfUiEvent
    data object CancelConversion : WordToPdfUiEvent
    data object ClearError : WordToPdfUiEvent
    data object Reset : WordToPdfUiEvent
}
