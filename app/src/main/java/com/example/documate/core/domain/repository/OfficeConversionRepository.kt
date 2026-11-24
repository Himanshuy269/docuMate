package com.example.documate.core.domain.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentTool
import java.io.File

/**
 * Domain repository contract for Office document conversion (Excel, Word, PowerPoint to PDF).
 * Real conversions communicate with an isolated LibreOffice backend service.
 */
interface OfficeConversionRepository {
    /**
     * Converts an office document file to a PDF.
     * Requires explicit user consent before uploading to the backend.
     */
    suspend fun convertOfficeDocumentToPdf(
        sourceFile: File,
        tool: DocumentTool,
        backendBaseUrl: String,
        authToken: String? = null,
        onProgress: (Float) -> Unit = {}
    ): AppResult<File>

    /**
     * Checks if the configured backend service is reachable and healthy.
     */
    suspend fun checkBackendHealth(backendBaseUrl: String): AppResult<Boolean>
}
