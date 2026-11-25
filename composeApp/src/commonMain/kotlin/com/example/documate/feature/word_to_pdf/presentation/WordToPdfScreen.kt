package com.example.documate.feature.word_to_pdf.presentation

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
import com.example.documate.core.ui.rememberPlatformActions
import com.example.documate.core.ui.rememberPlatformDocumentSaver
import com.example.documate.core.ui.rememberPlatformFilePicker

@Composable
fun WordToPdfScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordToPdfViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val platformActions = rememberPlatformActions()

    val pickWordFile = rememberPlatformFilePicker(
        mimeType = "*/*"
    ) { files ->
        files.firstOrNull()?.let {
            viewModel.onEvent(WordToPdfUiEvent.SelectFile(it.uriString))
        }
    }

    val savePdf = rememberPlatformDocumentSaver(
        suggestedName = uiState.outputFileName,
        mimeType = "application/pdf"
    ) { dest ->
        viewModel.onEvent(WordToPdfUiEvent.SavePdfToUri(dest))
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
                    val targetUri = uiState.shareableUriString ?: uiState.resultFile!!.uriString
                    DocuResultCard(
                        resultFile = uiState.resultFile!!,
                        originalFileSizeBytes = uiState.selectedFile?.sizeBytes,
                        onPreview = { platformActions.openFile(targetUri, "application/pdf") },
                        onShare = { platformActions.shareFile(targetUri, "application/pdf") },
                        onSaveToStorage = { savePdf() },
                        onConvertAnother = { viewModel.onEvent(WordToPdfUiEvent.Reset) }
                    )
                }
            } else if (!uiState.isConverting) {
                // File selection
                item {
                    DocuSectionHeader(title = "Select Word Document")
                    if (uiState.selectedFile == null) {
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            DocuEmptyState(
                                title = "Select Word Document",
                                description = "Choose a .docx or .doc document to convert into high-fidelity PDF format.",
                                icon = Icons.Default.Description
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DocuPrimaryButton(
                            text = "Choose Word File",
                            onClick = { pickWordFile() },
                            icon = Icons.Default.Description
                        )
                    } else {
                        DocuFileCard(
                            file = uiState.selectedFile!!,
                            onRemove = { viewModel.onEvent(WordToPdfUiEvent.RemoveFile) }
                        )
                    }
                }

                if (uiState.selectedFile != null) {
                    // Conversion Options
                    item {
                        DocuSectionHeader(title = "Conversion Settings")
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Preserve Hyperlinks",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Keep active web and document links clickable in PDF output",
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

                    // Backend Consent
                    item {
                        DocuConsentCard(
                            hasConsented = uiState.hasConsentedToBackend,
                            onConsentChanged = { viewModel.onEvent(WordToPdfUiEvent.SetConsent(it)) },
                            backendBaseUrl = uiState.backendBaseUrl
                        )
                    }

                    // Convert Button
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
