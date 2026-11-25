package com.example.documate.core.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
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

    override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<PlatformFile> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val cacheDir = File(context.cacheDir, "documate_scratch").apply { mkdirs() }
            val cleanName = preferredName.ifBlank { "temp_${System.currentTimeMillis()}" }
            val sanitizedName = cleanName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(cacheDir, "${System.currentTimeMillis()}_$sanitizedName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext AppResult.Error("Unable to open input stream for: $uriString")

            AppResult.Success(
                PlatformFile(
                    path = targetFile.absolutePath,
                    name = targetFile.name,
                    sizeBytes = targetFile.length()
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to cache document: ${e.message}", e)
        }
    }

    override suspend fun exportFile(sourceFile: PlatformFile, destinationFileName: String): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val documentsDir = File(context.filesDir, "saved_documents").apply { mkdirs() }
            val targetFile = File(documentsDir, destinationFileName)
            val javaSource = File(sourceFile.path)
            javaSource.copyTo(targetFile, overwrite = true)
            AppResult.Success(targetFile.absolutePath)
        } catch (e: Exception) {
            AppResult.Error("Failed to export document: ${e.message}", e)
        }
    }

    override suspend fun copyFileToUri(sourceFile: PlatformFile, destinationUriString: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val destinationUri = Uri.parse(destinationUriString)
            val javaSource = File(sourceFile.path)
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                javaSource.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return@withContext AppResult.Error("Unable to open destination stream")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to save to destination: ${e.message}", e)
        }
    }

    override fun getShareableUri(file: PlatformFile): AppResult<String> {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, File(file.path))
            AppResult.Success(uri.toString())
        } catch (e: Exception) {
            AppResult.Error("Failed to generate shareable URI: ${e.message}", e)
        }
    }

    override suspend fun clearTemporaryFiles(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "documate_scratch")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to clear temporary files: ${e.message}", e)
        }
    }
}
