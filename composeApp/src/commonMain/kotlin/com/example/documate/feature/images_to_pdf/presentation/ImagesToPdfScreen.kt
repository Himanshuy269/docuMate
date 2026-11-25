package com.example.documate.feature.images_to_pdf.presentation

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.ui.AppViewModelProvider
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuEmptyState
import com.example.documate.core.ui.components.DocuFilterChip
import com.example.documate.core.ui.components.DocuPrimaryButton
import com.example.documate.core.ui.components.DocuProgressCard
import com.example.documate.core.ui.components.DocuResultCard
import com.example.documate.core.ui.components.DocuSecondaryButton
import com.example.documate.core.ui.components.DocuSectionHeader
import com.example.documate.core.ui.components.DocuThumbnailImage
import com.example.documate.core.ui.components.DocuTopAppBar
import com.example.documate.core.ui.rememberPlatformActions
import com.example.documate.core.ui.rememberPlatformDocumentSaver
import com.example.documate.core.ui.rememberPlatformFilePicker

@Composable
fun ImagesToPdfScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImagesToPdfViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val platformActions = rememberPlatformActions()

    val pickImages = rememberPlatformFilePicker(
        allowedExtensions = listOf("jpg", "jpeg", "png", "webp"),
        allowMultiple = true
    ) { files ->
        if (files.isNotEmpty()) {
            viewModel.onEvent(ImagesToPdfUiEvent.AddImages(files.map { it.uriString }))
        }
    }

    val savePdf = rememberPlatformDocumentSaver(
        suggestedName = uiState.outputFileName,
        mimeType = "application/pdf"
    ) { dest ->
        viewModel.onEvent(ImagesToPdfUiEvent.SavePdfToUri(dest))
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(ImagesToPdfUiEvent.ClearError)
        }
    }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(ImagesToPdfUiEvent.ClearSaveSuccess)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "Images to PDF",
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
            // Processing state
            if (uiState.isProcessing) {
                item {
                    DocuProgressCard(
                        status = uiState.statusMessage ?: "Converting images to PDF...",
                        progress = uiState.progress,
                        onCancel = { viewModel.onEvent(ImagesToPdfUiEvent.CancelConversion) }
                    )
                }
            }

            // Success result state
            if (uiState.resultFile != null) {
                item {
                    val targetUri = uiState.shareableUriString ?: uiState.resultFile!!.uriString
                    DocuResultCard(
                        resultFile = uiState.resultFile!!,
                        originalFileSizeBytes = uiState.selectedImages.sumOf { it.sizeBytes },
                        onPreview = { platformActions.openFile(targetUri, "application/pdf") },
                        onShare = { platformActions.shareFile(targetUri, "application/pdf") },
                        onSaveToStorage = { savePdf() },
                        onConvertAnother = { viewModel.onEvent(ImagesToPdfUiEvent.Reset) }
                    )
                }
            } else if (!uiState.isProcessing) {
                // Image list or Empty state
                if (uiState.selectedImages.isEmpty()) {
                    item {
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            DocuEmptyState(
                                title = "No Images Selected",
                                description = "Select multiple images from your device. You can reorder, preview, configure page size, orientation, and margins before converting.",
                                icon = Icons.Default.AddPhotoAlternate
                            )
                        }
                    }
                    item {
                        DocuPrimaryButton(
                            text = "Select Images",
                            onClick = { pickImages() },
                            icon = Icons.Default.AddPhotoAlternate
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DocuSectionHeader(title = "Selected Images (${uiState.selectedImages.size})")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.onEvent(ImagesToPdfUiEvent.ClearAllImages) }
                                ) {
                                    Text(
                                        text = "Clear All",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                DocuSecondaryButton(
                                    text = "Add More",
                                    onClick = { pickImages() },
                                    icon = Icons.Default.AddPhotoAlternate,
                                    modifier = Modifier.width(125.dp)
                                )
                            }
                        }
                    }

                    itemsIndexed(uiState.selectedImages, key = { index, image -> "${image.id}_$index" }) { index, image ->
                        DocuCard(modifier = Modifier.fillMaxWidth(), padding = 10.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                                )

                                DocuThumbnailImage(
                                    uriString = image.uriString,
                                    size = 54.dp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = image.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = image.formattedSize,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { viewModel.onEvent(ImagesToPdfUiEvent.MoveImage(index, index - 1)) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.onEvent(ImagesToPdfUiEvent.MoveImage(index, index + 1)) },
                                        enabled = index < uiState.selectedImages.size - 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.onEvent(ImagesToPdfUiEvent.RemoveImage(index)) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Configuration Options Card
                    item {
                        DocuCard(modifier = Modifier.fillMaxWidth()) {
                            DocuSectionHeader(title = "PDF Settings")
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Page Size",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PdfPageSize.entries.forEach { size ->
                                    DocuFilterChip(
                                        selected = uiState.pageSize == size,
                                        onClick = { viewModel.onEvent(ImagesToPdfUiEvent.SelectPageSize(size)) },
                                        label = size.displayName.substringBefore(" (")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Orientation",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PdfOrientation.entries.forEach { orientation ->
                                    DocuFilterChip(
                                        selected = uiState.orientation == orientation,
                                        onClick = { viewModel.onEvent(ImagesToPdfUiEvent.SelectOrientation(orientation)) },
                                        label = orientation.displayName
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Page Margins",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PdfMargin.entries.forEach { margin ->
                                    DocuFilterChip(
                                        selected = uiState.margin == margin,
                                        onClick = { viewModel.onEvent(ImagesToPdfUiEvent.SelectMargin(margin)) },
                                        label = margin.displayName
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = uiState.outputFileName,
                                onValueChange = { viewModel.onEvent(ImagesToPdfUiEvent.UpdateOutputFileName(it)) },
                                label = { Text("Output PDF Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Convert Button
                    item {
                        DocuPrimaryButton(
                            text = "Convert to PDF",
                            onClick = { viewModel.onEvent(ImagesToPdfUiEvent.StartConversion) },
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
