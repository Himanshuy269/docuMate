package com.example.documate.feature.history.presentation

import com.example.documate.core.domain.model.ToolCategory

sealed interface HistoryUiEvent {
    data class SelectFilter(val category: ToolCategory?) : HistoryUiEvent
    data class DeleteRecord(val id: String) : HistoryUiEvent
    data object ShowClearConfirmation : HistoryUiEvent
    data object DismissClearConfirmation : HistoryUiEvent
    data object ConfirmClearAll : HistoryUiEvent
    data object ClearError : HistoryUiEvent
}
