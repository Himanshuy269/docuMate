package com.example.documate.feature.history.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.core.ui.AppViewModelProvider
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuConfirmDialog
import com.example.documate.core.ui.components.DocuEmptyState
import com.example.documate.core.ui.components.DocuFilterChip
import com.example.documate.core.ui.components.DocuTopAppBar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(HistoryUiEvent.ClearError)
        }
    }

    if (uiState.isConfirmClearDialogOpen) {
        DocuConfirmDialog(
            title = "Clear All Conversion History?",
            message = "This will remove all saved conversion records from the list. The generated files in app storage will remain safe.",
            confirmButtonText = "Clear History",
            onConfirm = { viewModel.onEvent(HistoryUiEvent.ConfirmClearAll) },
            onDismiss = { viewModel.onEvent(HistoryUiEvent.DismissClearConfirmation) },
            isDestructive = true
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "Conversion History",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.records.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onEvent(HistoryUiEvent.ShowClearConfirmation) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all history"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocuFilterChip(
                    selected = uiState.selectedFilter == null,
                    onClick = { viewModel.onEvent(HistoryUiEvent.SelectFilter(null)) },
                    label = "All (${uiState.records.size})"
                )
                DocuFilterChip(
                    selected = uiState.selectedFilter == ToolCategory.PDF,
                    onClick = { viewModel.onEvent(HistoryUiEvent.SelectFilter(ToolCategory.PDF)) },
                    label = "PDF Tools"
                )
                DocuFilterChip(
                    selected = uiState.selectedFilter == ToolCategory.OFFICE,
                    onClick = { viewModel.onEvent(HistoryUiEvent.SelectFilter(ToolCategory.OFFICE)) },
                    label = "Office to PDF"
                )
            }

            if (uiState.filteredRecords.isEmpty()) {
                DocuCard(modifier = Modifier.weight(1f)) {
                    DocuEmptyState(
                        title = "No History Records",
                        description = if (uiState.records.isEmpty()) {
                            "When you convert or compress documents, records will be listed here."
                        } else {
                            "No records match the selected filter."
                        },
                        icon = Icons.Default.History
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredRecords, key = { it.id }) { record ->
                        HistoryRecordCard(
                            record = record,
                            onPreview = {
                                val file = File(record.outputPath)
                                if (!file.exists()) {
                                    Toast.makeText(context, "File no longer exists on disk", Toast.LENGTH_SHORT).show()
                                    return@HistoryRecordCard
                                }
                                try {
                                    val authority = "${context.packageName}.fileprovider"
                                    val uri = FileProvider.getUriForFile(context, authority, file)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open PDF"))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, "No PDF viewer application found", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to open file: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShare = {
                                val file = File(record.outputPath)
                                if (!file.exists()) {
                                    Toast.makeText(context, "File no longer exists on disk", Toast.LENGTH_SHORT).show()
                                    return@HistoryRecordCard
                                }
                                try {
                                    val authority = "${context.packageName}.fileprovider"
                                    val uri = FileProvider.getUriForFile(context, authority, file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, "No sharing application found", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to share file: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                viewModel.onEvent(HistoryUiEvent.DeleteRecord(record.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: ConversionRecord,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    DocuCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

                Spacer(modifier = Modifier.width(10.dp))

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

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Source: ${record.sourceFileName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (record.compressionRatioText != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = record.compressionRatioText ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Preview",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
