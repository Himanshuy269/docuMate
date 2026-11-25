package com.example.documate.feature.home.presentation

import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.ToolCategory

data class HomeUiState(
    val searchQuery: String = "",
    val selectedCategory: ToolCategory? = null,
    val recentConversions: List<ConversionRecord> = emptyList(),
    val isLoadingHistory: Boolean = false
) {
    val filteredTools: List<DocumentTool>
        get() = DocumentTool.entries.filter { tool ->
            val matchesCategory = selectedCategory == null || tool.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                tool.title.contains(searchQuery, ignoreCase = true) ||
                tool.subtitle.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

    val pdfTools: List<DocumentTool>
        get() = filteredTools.filter { it.category == ToolCategory.PDF }

    val officeTools: List<DocumentTool>
        get() = filteredTools.filter { it.category == ToolCategory.OFFICE }
}
