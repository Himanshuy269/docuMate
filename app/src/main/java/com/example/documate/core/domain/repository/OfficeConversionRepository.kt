package com.example.documate.core.domain.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentTool
import java.io.File

/**
 * Options configuring Office document conversion to PDF.
 */
data class OfficeConversionOptions(
    val preserveHyperlinks: Boolean = true,
    val fitToPageWidth: Boolean = true,
    val slidesPerPage: Int = 1,
    val outputFileName: String? = null
)

/**
 * Domain repository contract for Office document conversion (Excel, Word, PowerPoint to PDF).
 * Real conversions communicate with an isolated LibreOffice backend service or native rendering engine.
 */
interface OfficeConversionRepository {
    /**
     * Converts an office document file to a PDF.
     * Requires explicit user consent before uploading to the backend or converting.
     */
    suspend fun convertOfficeDocumentToPdf(
        sourceFile: File,
        tool: DocumentTool,
        backendBaseUrl: String,
        authToken: String? = null,
        options: OfficeConversionOptions = OfficeConversionOptions(),
        onProgress: (Float) -> Unit = {}
    ): AppResult<File>

    /**
     * Checks if the configured backend service is reachable and healthy.
     */
    suspend fun checkBackendHealth(backendBaseUrl: String): AppResult<Boolean>
}
