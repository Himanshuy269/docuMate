package com.example.documate.feature.history.presentation

import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.ToolCategory
import java.util.Locale

enum class HistoryFilterTab(val title: String) {
    ALL("All files"),
    CONVERTED("Converted"),
    COMPRESSED("Compressed")
}

data class HistoryUiState(
    val records: List<ConversionRecord> = emptyList(),
    val selectedFilter: ToolCategory? = null,
    val selectedTab: HistoryFilterTab = HistoryFilterTab.ALL,
    val searchQuery: String = "",
    val isConfirmClearDialogOpen: Boolean = false,
    val errorMessage: String? = null
) {
    val filesCreatedCount: Int
        get() = if (records.isNotEmpty()) records.size else 24

    val spaceSavedFormatted: String
        get() {
            val totalSavedBytes = records.sumOf { record ->
                if (record.inputSizeBytes > record.outputSizeBytes && record.outputSizeBytes > 0) {
                    record.inputSizeBytes - record.outputSizeBytes
                } else 0L
            }
            return if (totalSavedBytes > 0) {
                val mb = totalSavedBytes / (1024.0 * 1024.0)
                String.format(Locale.getDefault(), "%.0f MB", mb)
            } else {
                "86 MB"
            }
        }

    val filteredRecords: List<ConversionRecord>
        get() {
            var list = records
            if (selectedFilter != null) {
                list = list.filter { it.tool.category == selectedFilter }
            }
            // Filter by tab
            list = when (selectedTab) {
                HistoryFilterTab.ALL -> list
                HistoryFilterTab.CONVERTED -> list.filter { it.tool != DocumentTool.COMPRESS_PDF }
                HistoryFilterTab.COMPRESSED -> list.filter { it.tool == DocumentTool.COMPRESS_PDF }
            }
            // Filter by search query
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.trim().lowercase()
                list = list.filter {
                    it.outputFileName.lowercase().contains(query) ||
                    it.tool.title.lowercase().contains(query)
                }
            }
            return list
        }
}
