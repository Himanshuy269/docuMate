package com.example.documate.core.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Android implementation of DocumentRepository utilizing ContentResolver
 * and app-private isolated cache storage.
 */
class AndroidDocumentRepository(
    private val context: Context
) : DocumentRepository {

    override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            var displayName = "document_${System.currentTimeMillis()}"
            var sizeBytes = 0L
            val mimeType = context.contentResolver.getType(uri) ?: ""

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            val extension = displayName.substringAfterLast('.', "")
            val documentFile = DocumentFile(
                id = UUID.randomUUID().toString(),
                name = displayName,
                uriString = uriString,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                extension = extension
            )
            AppResult.Success(documentFile)
        } catch (e: Exception) {
            AppResult.Error("Failed to resolve document metadata: ${e.message}", e)
        }
    }

    override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val cacheDir = File(context.cacheDir, "documate_temp").apply { mkdirs() }
            val tempFile = File(cacheDir, "${System.currentTimeMillis()}_$preferredName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext AppResult.Error("Failed to read file stream: could not open input stream")

            AppResult.Success(tempFile)
        } catch (e: Exception) {
            AppResult.Error("Failed to copy file to temporary cache: ${e.message}", e)
        }
    }

    override suspend fun exportFile(sourceFile: File, destinationFileName: String): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
            val destFile = File(exportsDir, destinationFileName)
            sourceFile.copyTo(destFile, overwrite = true)
            AppResult.Success(destFile.absolutePath)
        } catch (e: Exception) {
            AppResult.Error("Failed to export document: ${e.message}", e)
        }
    }

    override suspend fun copyFileToUri(sourceFile: File, destinationUriString: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val destUri = Uri.parse(destinationUriString)
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return@withContext AppResult.Error("Could not open destination output stream")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to save file to destination: ${e.message}", e)
        }
    }

    override fun getShareableUri(file: File): AppResult<String> {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            AppResult.Success(uri.toString())
        } catch (e: Exception) {
            AppResult.Error("Failed to resolve shareable URI: ${e.message}", e)
        }
    }

    override suspend fun clearTemporaryFiles(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "documate_temp")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to clear temporary files: ${e.message}", e)
        }
    }
}
