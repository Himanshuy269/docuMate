package com.example.documate.feature.add_images_to_pdf.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.documate.core.ui.AppViewModelProvider
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuEmptyState
import com.example.documate.core.ui.components.DocuFileCard
import com.example.documate.core.ui.components.DocuFilterChip
import com.example.documate.core.ui.components.DocuPrimaryButton
import com.example.documate.core.ui.components.DocuProgressCard
import com.example.documate.core.ui.components.DocuResultCard
import com.example.documate.core.ui.components.DocuSecondaryButton
import com.example.documate.core.ui.components.DocuSectionHeader
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun AddImagesToPdfScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddImagesToPdfViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onEvent(AddImagesToPdfUiEvent.SelectPdf(it.toString())) }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onEvent(AddImagesToPdfUiEvent.AddImages(uris.map { it.toString() }))
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(AddImagesToPdfUiEvent.ClearError)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "Add Images to PDF",
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
            if (uiState.isProcessing) {
                item {
                    DocuProgressCard(
                        status = uiState.statusMessage ?: "Inserting images into PDF...",
                        progress = uiState.progress,
                        onCancel = { viewModel.onEvent(AddImagesToPdfUiEvent.CancelProcessing) }
                    )
                }
            }

            if (uiState.resultFile != null) {
                item {
                    DocuResultCard(
                        resultFile = uiState.resultFile!!,
                        originalFileSizeBytes = (uiState.selectedPdf?.sizeBytes ?: 0L) + uiState.selectedImages.sumOf { it.sizeBytes },
                        onPreview = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(uiState.resultFile!!.uriString), "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Preview PDF"))
                        },
                        onShare = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(uiState.resultFile!!.uriString))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share PDF"))
                        },
                        onConvertAnother = { viewModel.onEvent(AddImagesToPdfUiEvent.Reset) }
                    )
                }
            } else if (!uiState.isProcessing) {
                // Step 1: Source PDF
                item {
                    DocuSectionHeader(title = "1. Source PDF Document")
                    if (uiState.selectedPdf == null) {
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            DocuEmptyState(
                                title = "Select Existing PDF",
                                description = "Choose a PDF file from your device into which you want to insert images.",
                                icon = Icons.Default.PictureAsPdf
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DocuSecondaryButton(
                            text = "Choose PDF File",
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            icon = Icons.Default.PictureAsPdf
                        )
                    } else {
                        DocuFileCard(
                            file = uiState.selectedPdf!!,
                            onRemove = { viewModel.onEvent(AddImagesToPdfUiEvent.RemovePdf) }
                        )
                    }
                }

                // Step 2: Images to Insert
                item {
                    DocuSectionHeader(title = "2. Images to Insert (${uiState.selectedImages.size})")
                    if (uiState.selectedImages.isEmpty()) {
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            DocuEmptyState(
                                title = "No Images Selected",
                                description = "Choose one or more images to insert into your PDF document.",
                                icon = Icons.Default.AddPhotoAlternate
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DocuSecondaryButton(
                            text = "Choose Images",
                            onClick = { imagePickerLauncher.launch("image/*") },
                            icon = Icons.Default.AddPhotoAlternate
                        )
                    } else {
                        DocuSecondaryButton(
                            text = "Add More Images",
                            onClick = { imagePickerLauncher.launch("image/*") },
                            icon = Icons.Default.AddPhotoAlternate
                        )
                    }
                }

                itemsIndexed(uiState.selectedImages, key = { index, img -> "${img.id}_$index" }) { index, img ->
                    DocuCard(modifier = Modifier.fillMaxWidth(), padding = 10.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = img.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = img.formattedSize,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.onEvent(AddImagesToPdfUiEvent.RemoveImage(index)) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Step 3: Insert Position Settings
                if (uiState.selectedPdf != null && uiState.selectedImages.isNotEmpty()) {
                    item {
                        DocuSectionHeader(title = "3. Insertion Options")
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Where to insert images",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InsertPosition.entries.forEach { pos ->
                                    DocuFilterChip(
                                        selected = uiState.insertPosition == pos,
                                        onClick = { viewModel.onEvent(AddImagesToPdfUiEvent.SelectInsertPosition(pos)) },
                                        label = when (pos) {
                                            InsertPosition.AT_BEGINNING -> "Beginning"
                                            InsertPosition.AT_END -> "End"
                                            InsertPosition.SPECIFIC_PAGE -> "Page #"
                                        }
                                    )
                                }
                            }

                            if (uiState.insertPosition == InsertPosition.SPECIFIC_PAGE) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = uiState.specificPageNumber.toString(),
                                    onValueChange = {
                                        it.toIntOrNull()?.let { page ->
                                            viewModel.onEvent(AddImagesToPdfUiEvent.UpdatePageNumber(page))
                                        }
                                    },
                                    label = { Text("Insert after page number") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = uiState.outputFileName,
                                onValueChange = { viewModel.onEvent(AddImagesToPdfUiEvent.UpdateOutputFileName(it)) },
                                label = { Text("Output PDF Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Note: Original PDF is safely preserved. The merged document will be saved as a new file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        DocuPrimaryButton(
                            text = "Insert & Save New PDF",
                            onClick = { viewModel.onEvent(AddImagesToPdfUiEvent.StartProcessing) },
                            enabled = uiState.canProcess,
                            icon = Icons.Default.PictureAsPdf
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
