package com.example.documate.feature.pdf_to_excel.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun PdfToExcelScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfToExcelViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val platformActions = rememberPlatformActions()

    val pickPdfFile = rememberPlatformFilePicker(
        allowedExtensions = listOf("pdf")
    ) { files ->
        files.firstOrNull()?.let {
            viewModel.onEvent(PdfToExcelUiEvent.SelectFile(it.uriString))
        }
    }

    val saveXlsx = rememberPlatformDocumentSaver(
        suggestedName = uiState.outputFileName,
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ) { dest ->
        viewModel.onEvent(PdfToExcelUiEvent.SaveFileToUri(dest))
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(PdfToExcelUiEvent.ClearError)
        }
    }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(PdfToExcelUiEvent.ClearSaveSuccess)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "PDF to Excel",
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
                        status = uiState.statusMessage ?: "Converting PDF to Excel spreadsheet...",
                        progress = uiState.progress,
                        onCancel = { viewModel.onEvent(PdfToExcelUiEvent.CancelConversion) }
                    )
                }
            }

            if (uiState.resultFile != null) {
                item {
                    val targetUri = uiState.shareableUriString ?: uiState.resultFile!!.uriString
                    DocuResultCard(
                        resultFile = uiState.resultFile!!,
                        originalFileSizeBytes = uiState.selectedFile?.sizeBytes,
                        onPreview = { platformActions.openFile(targetUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                        onShare = { platformActions.shareFile(targetUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                        onSaveToStorage = { saveXlsx() },
                        onConvertAnother = { viewModel.onEvent(PdfToExcelUiEvent.Reset) }
                    )
                }
            } else if (!uiState.isConverting) {
                // File selection
                item {
                    DocuSectionHeader(title = "Select PDF Document")
                    if (uiState.selectedFile == null) {
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            DocuEmptyState(
                                title = "Select PDF Document",
                                description = "Choose a .pdf file with tables or tabular data to convert into an Excel (.xlsx) spreadsheet.",
                                icon = Icons.Default.PictureAsPdf
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DocuPrimaryButton(
                            text = "Choose PDF File",
                            onClick = { pickPdfFile() },
                            icon = Icons.Default.PictureAsPdf
                        )
                    } else {
                        DocuFileCard(
                            file = uiState.selectedFile!!,
                            onRemove = { viewModel.onEvent(PdfToExcelUiEvent.RemoveFile) }
                        )
                    }
                }

                if (uiState.selectedFile != null) {
                    // Conversion Options
                    item {
                        DocuSectionHeader(title = "Conversion Settings")
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.outputFileName,
                                onValueChange = { viewModel.onEvent(PdfToExcelUiEvent.UpdateOutputFileName(it)) },
                                label = { Text("Output XLSX Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Backend Consent
                    item {
                        DocuConsentCard(
                            hasConsented = uiState.hasConsentedToBackend,
                            onConsentChanged = { viewModel.onEvent(PdfToExcelUiEvent.SetConsent(it)) },
                            formatName = "PDF document"
                        )
                    }

                    // Convert Button
                    item {
                        DocuPrimaryButton(
                            text = "Convert to Excel",
                            onClick = { viewModel.onEvent(PdfToExcelUiEvent.StartConversion) },
                            enabled = uiState.canConvert,
                            icon = Icons.Default.TableChart
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
