package com.example.documate.feature.powerpoint_to_pdf.presentation

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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.MaterialTheme
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
import com.example.documate.core.ui.components.DocuFilterChip
import com.example.documate.core.ui.components.DocuPrimaryButton
import com.example.documate.core.ui.components.DocuProgressCard
import com.example.documate.core.ui.components.DocuResultCard
import com.example.documate.core.ui.components.DocuSectionHeader
import com.example.documate.core.ui.components.DocuTopAppBar
import com.example.documate.core.ui.rememberPlatformActions
import com.example.documate.core.ui.rememberPlatformDocumentSaver
import com.example.documate.core.ui.rememberPlatformFilePicker

@Composable
fun PowerPointToPdfScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PowerPointToPdfViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val platformActions = rememberPlatformActions()

    val pickPptFile = rememberPlatformFilePicker(
        mimeType = "*/*"
    ) { files ->
        files.firstOrNull()?.let {
            viewModel.onEvent(PowerPointToPdfUiEvent.SelectFile(it.uriString))
        }
    }

    val savePdf = rememberPlatformDocumentSaver(
        suggestedName = uiState.outputFileName,
        mimeType = "application/pdf"
    ) { dest ->
        viewModel.onEvent(PowerPointToPdfUiEvent.SavePdfToUri(dest))
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(PowerPointToPdfUiEvent.ClearError)
        }
    }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(PowerPointToPdfUiEvent.ClearSaveSuccess)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "PowerPoint to PDF",
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
                        status = uiState.statusMessage ?: "Converting PowerPoint slides to PDF...",
                        progress = uiState.progress,
                        onCancel = { viewModel.onEvent(PowerPointToPdfUiEvent.CancelConversion) }
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
                        onConvertAnother = { viewModel.onEvent(PowerPointToPdfUiEvent.Reset) }
                    )
                }
            } else if (!uiState.isConverting) {
                // File Selection
                item {
                    DocuSectionHeader(title = "Select PowerPoint Presentation")
                    if (uiState.selectedFile == null) {
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            DocuEmptyState(
                                title = "Select Presentation",
                                description = "Choose a .pptx or .ppt presentation to convert your slides into a clean PDF document.",
                                icon = Icons.Default.Slideshow
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DocuPrimaryButton(
                            text = "Choose PowerPoint File",
                            onClick = { pickPptFile() },
                            icon = Icons.Default.Slideshow
                        )
                    } else {
                        DocuFileCard(
                            file = uiState.selectedFile!!,
                            onRemove = { viewModel.onEvent(PowerPointToPdfUiEvent.RemoveFile) }
                        )
                    }
                }

                if (uiState.selectedFile != null) {
                    // Slide layout options
                    item {
                        DocuSectionHeader(title = "Slide Layout Settings")
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Slides per PDF page",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1, 2, 4, 6).forEach { count ->
                                    DocuFilterChip(
                                        selected = uiState.slidesPerPage == count,
                                        onClick = { viewModel.onEvent(PowerPointToPdfUiEvent.UpdateSlidesPerPage(count)) },
                                        label = if (count == 1) "1 (Full)" else "$count Slides"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = uiState.outputFileName,
                                onValueChange = { viewModel.onEvent(PowerPointToPdfUiEvent.UpdateOutputFileName(it)) },
                                label = { Text("Output PDF Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Consent
                    item {
                        DocuConsentCard(
                            hasConsented = uiState.hasConsentedToBackend,
                            onConsentChanged = { viewModel.onEvent(PowerPointToPdfUiEvent.SetConsent(it)) },
                            backendBaseUrl = uiState.backendBaseUrl
                        )
                    }

                    // Convert Button
                    item {
                        DocuPrimaryButton(
                            text = "Convert to PDF",
                            onClick = { viewModel.onEvent(PowerPointToPdfUiEvent.StartConversion) },
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
