package com.example.documate.core.domain.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import java.io.File

/**
 * Domain repository contract for on-device PDF generation, manipulation, and compression.
 */
interface PdfRepository {
    /**
     * Converts a collection of images into a single PDF document.
     */
    suspend fun createPdfFromImages(
        imageFiles: List<File>,
        pageSize: PdfPageSize,
        orientation: PdfOrientation,
        outputFileName: String,
        margin: PdfMargin = PdfMargin.NONE,
        onProgress: (Float) -> Unit = {}
    ): AppResult<File>

    /**
     * Inserts images into an existing PDF at a specific page position.
     */
    suspend fun addImagesToPdf(
        existingPdfFile: File,
        imageFiles: List<File>,
        insertPositionPage: Int,
        outputFileName: String,
        onProgress: (Float) -> Unit = {}
    ): AppResult<File>

    /**
     * Compresses an existing PDF using image downsampling and stream compression.
     */
    suspend fun compressPdf(
        sourcePdfFile: File,
        preset: CompressionPreset,
        outputFileName: String,
        onProgress: (Float) -> Unit = {}
    ): AppResult<File>
}
