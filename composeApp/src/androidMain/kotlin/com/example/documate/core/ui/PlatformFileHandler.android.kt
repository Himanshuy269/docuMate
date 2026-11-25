package com.example.documate.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
import java.io.File

class AndroidPlatformActions(private val context: Context) : PlatformActions {
    override fun openFile(fileUriOrPath: String, mimeType: String) {
        try {
            val uri = resolveUri(fileUriOrPath)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Document").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to open this document", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun shareFile(fileUriOrPath: String, mimeType: String) {
        try {
            val uri = resolveUri(fileUriOrPath)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Document").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No sharing application found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun saveFileToDisk(file: PlatformFile, destinationUriOrName: String, onComplete: (Boolean) -> Unit) {
        try {
            val destUri = Uri.parse(destinationUriOrName)
            val sourceFile = File(file.path)
            if (sourceFile.exists()) {
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                onComplete(true)
            } else {
                onComplete(false)
            }
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    private fun resolveUri(pathOrUri: String): Uri {
        return if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
            Uri.parse(pathOrUri)
        } else {
            val file = File(pathOrUri)
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        }
    }
}

@Composable
actual fun rememberPlatformFilePicker(
    allowMultiple: Boolean,
    allowedExtensions: List<String>,
    onFilesPicked: (List<DocumentFile>) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val mimeType = if (allowedExtensions.any { it.contains("pdf") }) "application/pdf"
    else if (allowedExtensions.any { it.contains("png") || it.contains("jpg") || it.contains("jpeg") }) "image/*"
    else "*/*"

    if (allowMultiple) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris ->
            val list = uris.map { uri ->
                resolveDocumentFile(context, uri)
            }
            onFilesPicked(list)
        }
        return { launcher.launch(mimeType) }
    } else {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                onFilesPicked(listOf(resolveDocumentFile(context, uri)))
            }
        }
        return { launcher.launch(mimeType) }
    }
}

@Composable
actual fun rememberPlatformDocumentSaver(
    suggestedName: String,
    mimeType: String,
    onDestinationSelected: (String) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { uri ->
        if (uri != null) {
            onDestinationSelected(uri.toString())
        }
    }
    return {
        val cleanName = if (suggestedName.endsWith(".pdf", ignoreCase = true)) suggestedName else "$suggestedName.pdf"
        launcher.launch(cleanName)
    }
}

@Composable
actual fun rememberPlatformActions(): PlatformActions {
    val context = LocalContext.current
    return remember(context) { AndroidPlatformActions(context) }
}

private fun resolveDocumentFile(context: Context, uri: Uri): DocumentFile {
    var displayName = "file_${System.currentTimeMillis()}"
    var sizeBytes = 0L
    val mime = context.contentResolver.getType(uri) ?: ""

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: displayName
            if (sizeIdx != -1) sizeBytes = cursor.getLong(sizeIdx)
        }
    }
    val ext = displayName.substringAfterLast('.', "")
    return DocumentFile(
        id = uri.toString(),
        name = displayName,
        uriString = uri.toString(),
        sizeBytes = sizeBytes,
        mimeType = mime,
        extension = ext
    )
}
