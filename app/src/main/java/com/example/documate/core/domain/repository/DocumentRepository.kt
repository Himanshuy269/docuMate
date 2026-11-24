package com.example.documate.core.domain.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import java.io.File

/**
 * Domain repository contract for cross-platform file picking, temporary caching,
 * saving, and document sharing.
 */
interface DocumentRepository {
    /**
     * Resolves metadata (display name, size, mime type) from a platform URI string.
     */
    suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile>

    /**
     * Copies a file to secure, app-private cache for processing.
     */
    suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File>

    /**
     * Saves a processed document file to platform document storage.
     */
    suspend fun exportFile(sourceFile: File, destinationFileName: String): AppResult<String>

    /**
     * Cleans up temporary working files created during processing.
     */
    suspend fun clearTemporaryFiles(): AppResult<Unit>
}
