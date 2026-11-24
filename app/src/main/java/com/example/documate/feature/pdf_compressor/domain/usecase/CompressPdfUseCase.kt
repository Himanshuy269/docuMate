package com.example.documate.feature.pdf_compressor.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Domain Use Case encapsulating the complete pipeline for compressing a PDF document:
 * 1. Validating input arguments (source PDF existence, output file name).
 * 2. Caching source PDF into isolated app scratch storage.
 * 3. Calling the on-device native PDF compression engine with image downsampling and stream compression.
 * 4. Calculating space savings and recording conversion history.
 * 5. Cleaning up intermediate scratch files.
 */
class CompressPdfUseCase(
    private val documentRepository: DocumentRepository,
    private val pdfRepository: PdfRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        sourcePdf: DocumentFile?,
        preset: CompressionPreset,
        outputFileName: String,
        onProgress: (progress: Float, stage: String) -> Unit
    ): AppResult<DocumentFile> = withContext(Dispatchers.IO) {
        if (sourcePdf == null) {
            return@withContext AppResult.Error("Please select a PDF to compress")
        }

        val cleanName = outputFileName.trim().ifBlank {
            "${sourcePdf.name.substringBeforeLast('.')}_compressed"
        }

        onProgress(0.05f, "Preparing PDF for compression...")

        var cachedPdf: File? = null
        try {
            // Stage 1: Copy to scratch cache
            val copyResult = documentRepository.copyToAppCache(sourcePdf.uriString, sourcePdf.name)
            cachedPdf = when (copyResult) {
                is AppResult.Success -> copyResult.data
                is AppResult.Error -> {
                    return@withContext AppResult.Error(
                        "Failed to access source PDF: ${copyResult.message}",
                        copyResult.cause
                    )
                }
                else -> return@withContext AppResult.Error("Failed to cache source PDF")
            }

            // Stage 2: Perform native PDF compression
            onProgress(0.20f, "Analyzing PDF streams & compressing pages (${preset.title})...")
            val compressionResult = pdfRepository.compressPdf(
                sourcePdfFile = cachedPdf,
                preset = preset,
                outputFileName = cleanName,
                onProgress = { fraction ->
                    val progress = 0.20f + (0.75f * fraction)
                    val percent = (progress * 100).toInt()
                    onProgress(progress, "Compressing document pages ($percent%)...")
                }
            )

            when (compressionResult) {
                is AppResult.Success -> {
                    val outputFile = compressionResult.data
                    val resultDoc = DocumentFile(
                        id = UUID.randomUUID().toString(),
                        name = outputFile.name,
                        uriString = outputFile.toURI().toString(),
                        sizeBytes = outputFile.length(),
                        mimeType = "application/pdf",
                        extension = "pdf"
                    )

                    onProgress(0.96f, "Saving results and updating history...")
                    val record = ConversionRecord(
                        id = UUID.randomUUID().toString(),
                        tool = DocumentTool.PDF_COMPRESSION,
                        sourceFileName = sourcePdf.name,
                        outputFileName = outputFile.name,
                        outputPath = outputFile.absolutePath,
                        originalSizeBytes = sourcePdf.sizeBytes,
                        outputSizeBytes = outputFile.length(),
                        timestampMillis = System.currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    onProgress(1.0f, "Complete!")
                    AppResult.Success(resultDoc)
                }
                is AppResult.Error -> AppResult.Error(compressionResult.message, compressionResult.cause)
                else -> AppResult.Error("Unknown error occurred during PDF compression")
            }
        } finally {
            try {
                if (cachedPdf != null && cachedPdf.exists()) {
                    cachedPdf.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
