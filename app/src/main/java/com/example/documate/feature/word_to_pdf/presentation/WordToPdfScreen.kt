package com.example.documate.feature.word_to_pdf.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.documate.core.ui.AppViewModelProvider
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuConsentCard
import com.example.documate.core.ui.components.DocuEmptyState
import com.example.documate.core.ui.components.DocuFileCard
import com.example.documate.core.ui.components.DocuPrimaryButton
import com.example.documate.core.ui.components.DocuProgressCard
import com.example.documate.core.ui.components.DocuResultCard
import com.example.documate.core.ui.components.DocuSectionHeader
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun WordToPdfScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordToPdfViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onEvent(WordToPdfUiEvent.SelectFile(it.toString())) }
    }

    val saveDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            viewModel.onEvent(WordToPdfUiEvent.SavePdfToUri(it.toString()))
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(WordToPdfUiEvent.ClearError)
        }
    }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(WordToPdfUiEvent.ClearSaveSuccess)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "Word to PDF",
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
            if (uiState.isConverting) {
                item {
                    DocuProgressCard(
                        status = uiState.statusMessage ?: "Converting Word document to PDF...",
                        progress = uiState.progress,
                        onCancel = { viewModel.onEvent(WordToPdfUiEvent.CancelConversion) }
                    )
                }
            }

            if (uiState.resultFile != null) {
                item {
                    DocuResultCard(
                        resultFile = uiState.resultFile!!,
                        originalFileSizeBytes = uiState.selectedFile?.sizeBytes,
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
                        onConvertAnother = { viewModel.onEvent(WordToPdfUiEvent.Reset) }
                    )
                }
            } else if (!uiState.isConverting) {
                item {
                    DocuSectionHeader(title = "Select Word Document")
                    if (uiState.selectedFile == null) {
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            DocuEmptyState(
                                title = "Choose Document (.docx, .doc)",
                                description = "Select a Word file to preserve typography, styling, tables, and images into a standardized PDF.",
                                icon = Icons.Default.Description
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DocuPrimaryButton(
                            text = "Select Word Document",
                            onClick = { filePickerLauncher.launch("*/*") },
                            icon = Icons.Default.Description
                        )
                    } else {
                        DocuFileCard(
                            file = uiState.selectedFile!!,
                            onRemove = { viewModel.onEvent(WordToPdfUiEvent.RemoveFile) }
                        )
                    }
                }

                // Explicit Consent Card
                item {
                    DocuConsentCard(
                        consented = uiState.hasConsentedToBackend,
                        onConsentChange = { viewModel.onEvent(WordToPdfUiEvent.SetConsent(it)) },
                        formatName = "Word"
                    )
                }

                if (uiState.selectedFile != null) {
                    item {
                        DocuSectionHeader(title = "Document Options")
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Preserve clickable hyperlinks",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Retain embedded web URLs and internal document bookmarks in the output PDF.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.preserveHyperlinks,
                                    onCheckedChange = { viewModel.onEvent(WordToPdfUiEvent.TogglePreserveHyperlinks(it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = uiState.outputFileName,
                                onValueChange = { viewModel.onEvent(WordToPdfUiEvent.UpdateOutputFileName(it)) },
                                label = { Text("Output PDF Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        DocuPrimaryButton(
                            text = "Convert to PDF",
                            onClick = { viewModel.onEvent(WordToPdfUiEvent.StartConversion) },
                            enabled = uiState.canConvert,
                            icon = Icons.Default.PictureAsPdf
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
