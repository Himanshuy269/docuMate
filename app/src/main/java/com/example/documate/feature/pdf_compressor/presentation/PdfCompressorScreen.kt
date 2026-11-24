package com.example.documate.feature.pdf_compressor.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.ui.AppViewModelProvider
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuEmptyState
import com.example.documate.core.ui.components.DocuFileCard
import com.example.documate.core.ui.components.DocuPrimaryButton
import com.example.documate.core.ui.components.DocuProgressCard
import com.example.documate.core.ui.components.DocuResultCard
import com.example.documate.core.ui.components.DocuSectionHeader
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun PdfCompressorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfCompressorViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onEvent(PdfCompressorUiEvent.SelectPdf(it.toString())) }
    }

    val saveDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            viewModel.onEvent(PdfCompressorUiEvent.SavePdfToUri(it.toString()))
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(PdfCompressorUiEvent.ClearError)
        }
    }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(PdfCompressorUiEvent.ClearSaveSuccess)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "PDF Compression",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isCompressing) {
                item {
                    DocuProgressCard(
                        status = uiState.statusMessage ?: "Compressing PDF...",
                        progress = uiState.progress,
                        onCancel = { viewModel.onEvent(PdfCompressorUiEvent.CancelCompression) }
                    )
                }
            }

            if (uiState.resultFile != null) {
                item {
                    DocuResultCard(
                        resultFile = uiState.resultFile!!,
                        originalFileSizeBytes = uiState.selectedPdf?.sizeBytes,
                        onPreview = {
                            try {
                                val targetUri = Uri.parse(uiState.shareableUriString ?: uiState.resultFile!!.uriString)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(targetUri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Preview PDF"))
                            } catch (_: Exception) {}
                        },
                        onShare = {
                            try {
                                val targetUri = Uri.parse(uiState.shareableUriString ?: uiState.resultFile!!.uriString)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, targetUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share PDF"))
                            } catch (_: Exception) {}
                        },
                        onSaveToStorage = {
                            saveDocLauncher.launch(uiState.resultFile!!.name)
                        },
                        onConvertAnother = { viewModel.onEvent(PdfCompressorUiEvent.Reset) }
                    )
                }
            } else if (!uiState.isCompressing) {
                // PDF selection card
                item {
                    DocuSectionHeader(title = "Select PDF Document")
                    if (uiState.selectedPdf == null) {
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            DocuEmptyState(
                                title = "Select PDF to Compress",
                                description = "Choose a PDF file to reduce its file size while maintaining readability.",
                                icon = Icons.Default.PictureAsPdf
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DocuPrimaryButton(
                            text = "Choose PDF File",
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            icon = Icons.Default.PictureAsPdf
                        )
                    } else {
                        DocuFileCard(
                            file = uiState.selectedPdf!!,
                            onRemove = { viewModel.onEvent(PdfCompressorUiEvent.RemovePdf) }
                        )
                        if (uiState.pdfPageCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Document contains ${uiState.pdfPageCount} page${if (uiState.pdfPageCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // Privacy guarantee note
                item {
                    DocuCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = 12.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = "100% On-Device Compression",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Your document is compressed locally and never uploaded to any server.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Presets & Settings
                if (uiState.selectedPdf != null) {
                    item {
                        DocuSectionHeader(title = "Compression Level")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompressionPreset.entries.forEach { preset ->
                                val isSelected = uiState.selectedPreset == preset
                                DocuCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onEvent(PdfCompressorUiEvent.SelectPreset(preset)) },
                                    padding = 12.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.onEvent(PdfCompressorUiEvent.SelectPreset(preset)) },
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = preset.title,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = preset.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        DocuSectionHeader(title = "Output Settings")
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.outputFileName,
                                onValueChange = { viewModel.onEvent(PdfCompressorUiEvent.UpdateOutputFileName(it)) },
                                label = { Text("Output PDF Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Safety Guarantee: Original file is never overwritten.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    item {
                        DocuPrimaryButton(
                            text = "Compress PDF",
                            onClick = { viewModel.onEvent(PdfCompressorUiEvent.StartCompression) },
                            enabled = uiState.canCompress,
                            icon = Icons.Default.Compress
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
