package com.example.documate.core.data.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.PdfRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.PDFKit.PDFDocument
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
class IosPdfRepository : PdfRepository {

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
            onProgress(0.2f)
            val fileManager = NSFileManager.defaultManager
            val cacheUrls = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            val cacheDirUrl = cacheUrls.firstOrNull() as? NSURL ?: return@withContext AppResult.Error("Unable to locate cache directory")
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val targetUrl = cacheDirUrl.URLByAppendingPathComponent(cleanName)
                ?: return@withContext AppResult.Error("Failed to build output URL")

            val pdfDocument = PDFDocument()
            for ((index, imgFile) in imageFiles.withIndex()) {
                val uiImage = UIImage.imageWithContentsOfFile(imgFile.path)
                if (uiImage != null) {
                    val page = platform.PDFKit.PDFPage(uiImage)
                    if (page != null) {
                        pdfDocument.insertPage(page, atIndex = pdfDocument.pageCount)
                    }
                }
                onProgress(0.2f + 0.7f * ((index + 1).toFloat() / imageFiles.size))
            }

            pdfDocument.writeToURL(targetUrl)
            onProgress(1.0f)

            AppResult.Success(
                PlatformFile(
                    path = targetUrl.path ?: "",
                    name = cleanName,
                    sizeBytes = 0L
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to create PDF on iOS: ${e.message}", e)
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
            onProgress(0.2f)
            val sourceUrl = NSURL.fileURLWithPath(existingPdfFile.path)
            val pdfDoc = PDFDocument(sourceUrl) ?: return@withContext AppResult.Error("Unable to load source PDF")

            var insertIdx = insertPositionPage.toLong().coerceIn(0L, pdfDoc.pageCount)
            for (img in imageFiles) {
                val uiImg = UIImage.imageWithContentsOfFile(img.path)
                if (uiImg != null) {
                    val page = platform.PDFKit.PDFPage(uiImg)
                    if (page != null) {
                        pdfDoc.insertPage(page, atIndex = insertIdx)
                        insertIdx++
                    }
                }
            }

            val fileManager = NSFileManager.defaultManager
            val cacheUrls = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            val cacheDirUrl = cacheUrls.firstOrNull() as? NSURL ?: return@withContext AppResult.Error("Unable to locate cache directory")
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val targetUrl = cacheDirUrl.URLByAppendingPathComponent(cleanName) ?: return@withContext AppResult.Error("Failed to build target URL")

            pdfDoc.writeToURL(targetUrl)
            onProgress(1.0f)

            AppResult.Success(
                PlatformFile(
                    path = targetUrl.path ?: "",
                    name = cleanName,
                    sizeBytes = 0L
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to insert images into PDF: ${e.message}", e)
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
            val sourceUrl = NSURL.fileURLWithPath(sourcePdfFile.path)
            val pdfDoc = PDFDocument(sourceUrl) ?: return@withContext AppResult.Error("Unable to load PDF for compression")

            val fileManager = NSFileManager.defaultManager
            val cacheUrls = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            val cacheDirUrl = cacheUrls.firstOrNull() as? NSURL ?: return@withContext AppResult.Error("Unable to locate cache directory")
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val targetUrl = cacheDirUrl.URLByAppendingPathComponent(cleanName) ?: return@withContext AppResult.Error("Failed to build target URL")

            pdfDoc.writeToURL(targetUrl)
            onProgress(1.0f)

            AppResult.Success(
                PlatformFile(
                    path = targetUrl.path ?: "",
                    name = cleanName,
                    sizeBytes = 0L
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to compress PDF: ${e.message}", e)
        }
    }

    override suspend fun getPdfPageCount(pdfFile: PlatformFile): AppResult<Int> = withContext(Dispatchers.Default) {
        try {
            val sourceUrl = NSURL.fileURLWithPath(pdfFile.path)
            val pdfDoc = PDFDocument(sourceUrl)
            AppResult.Success(pdfDoc?.pageCount?.toInt() ?: 0)
        } catch (e: Exception) {
            AppResult.Error("Failed to get page count: ${e.message}", e)
        }
    }
}
