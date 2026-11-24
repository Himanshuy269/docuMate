package com.example.documate.feature.history.presentation

import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.ToolCategory

data class HistoryUiState(
    val records: List<ConversionRecord> = emptyList(),
    val selectedFilter: ToolCategory? = null,
    val isConfirmClearDialogOpen: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredRecords: List<ConversionRecord>
        get() = if (selectedFilter == null) records
        else records.filter { it.tool.category == selectedFilter }
}
