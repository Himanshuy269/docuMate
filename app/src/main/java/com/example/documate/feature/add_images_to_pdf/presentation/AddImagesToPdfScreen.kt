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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.material3.TextButton
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
import com.example.documate.core.ui.components.DocuThumbnailImage
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

    val saveDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            viewModel.onEvent(AddImagesToPdfUiEvent.SavePdfToUri(it.toString()))
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(AddImagesToPdfUiEvent.ClearError)
        }
    }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(AddImagesToPdfUiEvent.ClearSaveSuccess)
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
                    val totalOriginalSize = (uiState.selectedPdf?.sizeBytes ?: 0L) +
                            uiState.selectedImages.sumOf { it.sizeBytes }

                    DocuResultCard(
                        resultFile = uiState.resultFile!!,
                        originalFileSizeBytes = totalOriginalSize,
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

                // Step 2: Images to Insert
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DocuSectionHeader(title = "2. Images to Insert (${uiState.selectedImages.size})")
                        if (uiState.selectedImages.isNotEmpty()) {
                            TextButton(onClick = { viewModel.onEvent(AddImagesToPdfUiEvent.ClearAllImages) }) {
                                Text("Clear all", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

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
                    DocuCard(modifier = Modifier.fillMaxWidth(), padding = 8.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                            )

                            DocuThumbnailImage(
                                uriString = img.uriString,
                                modifier = Modifier.size(52.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

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

                            IconButton(
                                onClick = { viewModel.onEvent(AddImagesToPdfUiEvent.MoveImage(index, index - 1)) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move up"
                                )
                            }

                            IconButton(
                                onClick = { viewModel.onEvent(AddImagesToPdfUiEvent.MoveImage(index, index + 1)) },
                                enabled = index < uiState.selectedImages.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move down"
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
                                            InsertPosition.SPECIFIC_PAGE -> "After Page #"
                                        }
                                    )
                                }
                            }

                            if (uiState.insertPosition == InsertPosition.SPECIFIC_PAGE) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val maxPage = maxOf(1, uiState.pdfPageCount)
                                OutlinedTextField(
                                    value = uiState.specificPageNumber.toString(),
                                    onValueChange = { input ->
                                        input.toIntOrNull()?.let { page ->
                                            viewModel.onEvent(AddImagesToPdfUiEvent.UpdatePageNumber(page.coerceIn(1, maxPage)))
                                        }
                                    },
                                    label = { Text("Insert after page number (1 to $maxPage)") },
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
