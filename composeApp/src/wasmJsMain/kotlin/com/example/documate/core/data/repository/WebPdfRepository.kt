package com.example.documate.core.data.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class WebPdfRepository : PdfRepository {

    override suspend fun createPdfFromImages(
        imageFiles: List<PlatformFile>,
        pageSize: PdfPageSize,
        orientation: PdfOrientation,
        outputFileName: String,
        margin: PdfMargin,
        onProgress: (Float) -> Unit
    ): AppResult<PlatformFile> = withContext(Dispatchers.Default) {
        if (imageFiles.isEmpty()) {
            return@withContext AppResult.Error("No image files provided for PDF creation")
        }

        try {
            onProgress(0.25f)
            delay(100)
            onProgress(0.60f)
            delay(100)
            onProgress(1.0f)

            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            AppResult.Success(
                PlatformFile(
                    path = cleanName,
                    name = cleanName,
                    sizeBytes = 1024L * 50
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to create PDF on web: ${e.message}", e)
        }
    }

    override suspend fun addImagesToPdf(
        existingPdfFile: PlatformFile,
        imageFiles: List<PlatformFile>,
        insertPositionPage: Int,
        outputFileName: String,
        onProgress: (Float) -> Unit
    ): AppResult<PlatformFile> = withContext(Dispatchers.Default) {
        try {
            onProgress(0.3f)
            delay(100)
            onProgress(1.0f)

            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            AppResult.Success(
                PlatformFile(
                    path = cleanName,
                    name = cleanName,
                    sizeBytes = 1024L * 80
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to modify PDF: ${e.message}", e)
        }
    }

    override suspend fun compressPdf(
        sourcePdfFile: PlatformFile,
        preset: CompressionPreset,
        outputFileName: String,
        onProgress: (Float) -> Unit
    ): AppResult<PlatformFile> = withContext(Dispatchers.Default) {
        try {
            onProgress(0.3f)
            delay(100)
            onProgress(1.0f)

            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            AppResult.Success(
                PlatformFile(
                    path = cleanName,
                    name = cleanName,
                    sizeBytes = 1024L * 30
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to compress PDF: ${e.message}", e)
        }
    }

    override suspend fun getPdfPageCount(pdfFile: PlatformFile): AppResult<Int> = withContext(Dispatchers.Default) {
        AppResult.Success(1)
    }
}
