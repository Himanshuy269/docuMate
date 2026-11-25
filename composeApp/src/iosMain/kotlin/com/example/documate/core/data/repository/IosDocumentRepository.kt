package com.example.documate.core.data.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
class IosDocumentRepository : DocumentRepository {

    override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> = withContext(Dispatchers.Default) {
        val url = NSURL.fileURLWithPath(uriString)
        val name = url.lastPathComponent ?: "document"
        val ext = name.substringAfterLast('.', "")
        AppResult.Success(
            DocumentFile(
                id = uriString,
                name = name,
                uriString = uriString,
                sizeBytes = 0L,
                extension = ext
            )
        )
    }

    override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<PlatformFile> = withContext(Dispatchers.Default) {
        try {
            val fileManager = NSFileManager.defaultManager
            val cacheUrls = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            val cacheDirUrl = cacheUrls.firstOrNull() as? NSURL ?: return@withContext AppResult.Error("Unable to locate cache directory")
            val targetUrl = cacheDirUrl.URLByAppendingPathComponent("${kotlin.random.Random.nextInt(10000)}_$preferredName")
                ?: return@withContext AppResult.Error("Failed to build target cache URL")

            val sourceUrl = NSURL.fileURLWithPath(uriString)
            fileManager.copyItemAtURL(sourceUrl, targetUrl, null)

            val path = targetUrl.path ?: uriString
            AppResult.Success(
                PlatformFile(
                    path = path,
                    name = preferredName,
                    sizeBytes = 0L
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to cache document: ${e.message}", e)
        }
    }

    override suspend fun exportFile(sourceFile: PlatformFile, destinationFileName: String): AppResult<String> = withContext(Dispatchers.Default) {
        try {
            val fileManager = NSFileManager.defaultManager
            val docUrls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
            val docDirUrl = docUrls.firstOrNull() as? NSURL ?: return@withContext AppResult.Error("Unable to locate Documents directory")
            val targetUrl = docDirUrl.URLByAppendingPathComponent(destinationFileName)
                ?: return@withContext AppResult.Error("Failed to build destination URL")

            val sourceUrl = NSURL.fileURLWithPath(sourceFile.path)
            fileManager.copyItemAtURL(sourceUrl, targetUrl, null)

            AppResult.Success(targetUrl.path ?: destinationFileName)
        } catch (e: Exception) {
            AppResult.Error("Failed to export document: ${e.message}", e)
        }
    }

    override suspend fun copyFileToUri(sourceFile: PlatformFile, destinationUriString: String): AppResult<Unit> = withContext(Dispatchers.Default) {
        AppResult.Success(Unit)
    }

    override fun getShareableUri(file: PlatformFile): AppResult<String> {
        return AppResult.Success(file.path)
    }

    override suspend fun clearTemporaryFiles(): AppResult<Unit> = withContext(Dispatchers.Default) {
        AppResult.Success(Unit)
    }
}
