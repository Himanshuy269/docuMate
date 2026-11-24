package com.example.documate.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuFilterChip
import com.example.documate.core.ui.components.DocuSearchBar
import com.example.documate.core.ui.components.DocuSectionHeader
import com.example.documate.core.ui.components.DocuToolCard
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun HomeScreen(
    onNavigateToTool: (DocumentTool) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(null) }

    val filteredTools = remember(searchQuery, selectedCategory) {
        DocumentTool.entries.filter { tool ->
            val matchesCategory = selectedCategory == null || tool.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                tool.title.contains(searchQuery, ignoreCase = true) ||
                tool.subtitle.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val pdfTools = remember(filteredTools) {
        filteredTools.filter { it.category == ToolCategory.PDF }
    }

    val officeTools = remember(filteredTools) {
        filteredTools.filter { it.category == ToolCategory.OFFICE }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "DocuMate",
                canNavigateBack = false
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DocuSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search document tools..."
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DocuFilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = "All Tools"
                    )
                    DocuFilterChip(
                        selected = selectedCategory == ToolCategory.PDF,
                        onClick = {
                            selectedCategory = if (selectedCategory == ToolCategory.PDF) null else ToolCategory.PDF
                        },
                        label = "PDF Tools"
                    )
                    DocuFilterChip(
                        selected = selectedCategory == ToolCategory.OFFICE,
                        onClick = {
                            selectedCategory = if (selectedCategory == ToolCategory.OFFICE) null else ToolCategory.OFFICE
                        },
                        label = "Office to PDF"
                    )
                }
            }

            if (pdfTools.isNotEmpty()) {
                item {
                    DocuSectionHeader(title = "PDF Tools")
                }
                items(pdfTools, key = { it.id }) { tool ->
                    DocuToolCard(
                        tool = tool,
                        onClick = { onNavigateToTool(tool) }
                    )
                }
            }

            if (officeTools.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    DocuSectionHeader(title = "Office to PDF")
                }
                items(officeTools, key = { it.id }) { tool ->
                    DocuToolCard(
                        tool = tool,
                        onClick = { onNavigateToTool(tool) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                DocuSectionHeader(
                    title = "Recent Conversions",
                    actionText = "View All",
                    onActionClick = onNavigateToHistory
                )
                DocuCard(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No recent conversions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Documents you convert will appear here for easy access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
