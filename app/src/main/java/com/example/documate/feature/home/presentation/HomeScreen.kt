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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.core.ui.AppViewModelProvider
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuFilterChip
import com.example.documate.core.ui.components.DocuSearchBar
import com.example.documate.core.ui.components.DocuSectionHeader
import com.example.documate.core.ui.components.DocuToolCard
import com.example.documate.core.ui.components.DocuTopAppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToTool: (DocumentTool) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Search document tools..."
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DocuFilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.onSelectCategory(null) },
                        label = "All Tools"
                    )
                    DocuFilterChip(
                        selected = uiState.selectedCategory == ToolCategory.PDF,
                        onClick = { viewModel.onSelectCategory(ToolCategory.PDF) },
                        label = "PDF Tools"
                    )
                    DocuFilterChip(
                        selected = uiState.selectedCategory == ToolCategory.OFFICE,
                        onClick = { viewModel.onSelectCategory(ToolCategory.OFFICE) },
                        label = "Office to PDF"
                    )
                }
            }

            if (uiState.pdfTools.isNotEmpty()) {
                item {
                    DocuSectionHeader(title = "PDF Tools")
                }
                items(uiState.pdfTools, key = { it.id }) { tool ->
                    DocuToolCard(
                        tool = tool,
                        onClick = { onNavigateToTool(tool) }
                    )
                }
            }

            if (uiState.officeTools.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    DocuSectionHeader(title = "Office to PDF")
                }
                items(uiState.officeTools, key = { it.id }) { tool ->
                    DocuToolCard(
                        tool = tool,
                        onClick = { onNavigateToTool(tool) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                DocuSectionHeader(
                    title = "Recent Conversions",
                    actionText = if (uiState.recentConversions.isNotEmpty()) "View All" else null,
                    onActionClick = onNavigateToHistory
                )

                if (uiState.recentConversions.isEmpty()) {
                    DocuCard(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No recent conversions yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Documents you convert or compress will appear here for quick access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.recentConversions.forEach { record ->
                            RecentConversionItem(
                                record = record,
                                onClick = onNavigateToHistory
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RecentConversionItem(
    record: ConversionRecord,
    onClick: () -> Unit
) {
    DocuCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        padding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.outputFileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val formattedDate = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                    .format(Date(record.timestampMillis))
                Text(
                    text = "${record.tool.title} • $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.compressionRatioText != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = record.compressionRatioText ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
