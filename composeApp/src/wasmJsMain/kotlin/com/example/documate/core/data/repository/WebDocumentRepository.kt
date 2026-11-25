package com.example.documate.core.data.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebDocumentRepository : DocumentRepository {

    override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> = withContext(Dispatchers.Default) {
        val name = uriString.substringAfterLast('/', uriString)
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
        AppResult.Success(
            PlatformFile(
                path = uriString,
                name = preferredName,
                sizeBytes = 0L
            )
        )
    }

    override suspend fun exportFile(sourceFile: PlatformFile, destinationFileName: String): AppResult<String> = withContext(Dispatchers.Default) {
        AppResult.Success(destinationFileName)
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
