package com.example.documate.core.ui

import androidx.compose.runtime.Composable
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile

interface PlatformActions {
    fun openFile(fileUriOrPath: String, mimeType: String = "application/pdf")
    fun shareFile(fileUriOrPath: String, mimeType: String = "application/pdf")
    fun saveFileToDisk(file: PlatformFile, destinationUriOrName: String, onComplete: (Boolean) -> Unit)
}

@Composable
expect fun rememberPlatformFilePicker(
    allowMultiple: Boolean = false,
    allowedExtensions: List<String> = emptyList(),
    onFilesPicked: (List<DocumentFile>) -> Unit
): () -> Unit

@Composable
expect fun rememberPlatformDocumentSaver(
    suggestedName: String,
    mimeType: String = "application/pdf",
    onDestinationSelected: (String) -> Unit
): () -> Unit

@Composable
expect fun rememberPlatformActions(): PlatformActions
