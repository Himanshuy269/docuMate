package com.example.documate.core.domain.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.PlatformFile

/**
 * Domain repository contract for on-device PDF generation, manipulation, and compression.
 */
interface PdfRepository {
    /**
     * Converts a collection of images into a single PDF document.
     */
    suspend fun createPdfFromImages(
        imageFiles: List<PlatformFile>,
        pageSize: PdfPageSize,
        orientation: PdfOrientation,
        outputFileName: String,
        margin: PdfMargin = PdfMargin.NONE,
        onProgress: (Float) -> Unit = {}
    ): AppResult<PlatformFile>

    /**
     * Inserts images into an existing PDF at a specific page position.
     */
    suspend fun addImagesToPdf(
        existingPdfFile: PlatformFile,
        imageFiles: List<PlatformFile>,
        insertPositionPage: Int,
        outputFileName: String,
        onProgress: (Float) -> Unit = {}
    ): AppResult<PlatformFile>

    /**
     * Compresses an existing PDF using image downsampling and stream compression.
     */
    suspend fun compressPdf(
        sourcePdfFile: PlatformFile,
        preset: CompressionPreset,
        outputFileName: String,
        onProgress: (Float) -> Unit = {}
    ): AppResult<PlatformFile>

    /**
     * Inspects a PDF file and returns its total page count.
     */
    suspend fun getPdfPageCount(pdfFile: PlatformFile): AppResult<Int> = AppResult.Success(0)
}
