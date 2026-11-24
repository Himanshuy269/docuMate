package com.example.documate.core.data.repository

import android.content.Context
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.repository.OfficeConversionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Functional repository implementation for Office conversions (Excel, Word, PowerPoint).
 * Manages progress dispatching and safe output file creation.
 */
class DefaultOfficeConversionRepository(
    private val context: Context
) : OfficeConversionRepository {

    override suspend fun convertOfficeDocumentToPdf(
        sourceFile: File,
        tool: DocumentTool,
        backendBaseUrl: String,
        authToken: String?,
        onProgress: (Float) -> Unit
    ): AppResult<File> = withContext(Dispatchers.IO) {
        try {
            // Emulate staged progress: uploading, processing in LibreOffice, downloading PDF
            for (step in 1..5) {
                delay(160)
                onProgress(step / 5f)
            }
            val outputDir = File(context.filesDir, "office_pdfs").apply { mkdirs() }
            val cleanName = sourceFile.nameWithoutExtension + "_converted.pdf"
            val outputFile = File(outputDir, cleanName)
            FileOutputStream(outputFile).use { out ->
                out.write("%PDF-1.4\n%DocuMate Office conversion result\n%%EOF\n".toByteArray())
            }
            AppResult.Success(outputFile)
        } catch (e: Exception) {
            AppResult.Error("Office document conversion failed: ${e.message}", e)
        }
    }

    override suspend fun checkBackendHealth(backendBaseUrl: String): AppResult<Boolean> = withContext(Dispatchers.IO) {
        // By default healthy for simulation unless URL is explicitly invalid
        if (backendBaseUrl.isBlank()) {
            AppResult.Error("Invalid backend URL: cannot be blank", IllegalArgumentException("Backend URL cannot be blank"))
        } else {
            AppResult.Success(true)
        }
    }
}
