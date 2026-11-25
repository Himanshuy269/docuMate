package com.example.documate.feature.history.presentation

import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.ToolCategory

enum class HistoryFilterTab(val title: String) {
    ALL("All Files"),
    IMAGES("Images"),
    DOCUMENTS("Documents"),
    COMPRESSED("Compressed")
}

data class HistoryUiState(
    val records: List<ConversionRecord> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: HistoryFilterTab = HistoryFilterTab.ALL,
    val selectedToolFilter: DocumentTool? = null,
    val isSearchBarVisible: Boolean = false,
    val isClearHistoryDialogVisible: Boolean = false,
    val pendingDeleteRecordId: String? = null,
    val snackbarMessage: String? = null
) {
    val filteredRecords: List<ConversionRecord>
        get() = records.filter { record ->
            val matchesSearch = searchQuery.isBlank() ||
                    record.outputFileName.contains(searchQuery, ignoreCase = true) ||
                    record.sourceFileName.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedTab) {
                HistoryFilterTab.ALL -> true
                HistoryFilterTab.IMAGES -> record.tool == DocumentTool.IMAGES_TO_PDF || record.tool == DocumentTool.ADD_IMAGES_TO_PDF
                HistoryFilterTab.DOCUMENTS -> record.tool.category == ToolCategory.OFFICE_TO_PDF
                HistoryFilterTab.COMPRESSED -> record.tool == DocumentTool.PDF_COMPRESSOR
            }

            val matchesTool = selectedToolFilter == null || record.tool == selectedToolFilter

            matchesSearch && matchesTab && matchesTool
        }

    val totalConversionsCount: Int
        get() = records.size

    val formattedTotalStorageSaved: String
        get() {
            val totalSavedBytes = records
                .filter { it.originalSizeBytes > it.outputSizeBytes }
                .sumOf { it.originalSizeBytes - it.outputSizeBytes }

            return if (totalSavedBytes > 0) {
                val mb = totalSavedBytes / (1024.0 * 1024.0)
                "${mb.toLong()} MB"
            } else {
                "0 MB"
            }
        }
}
