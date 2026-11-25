package com.example.documate.feature.pdf_compressor.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.common.currentTimeMillis
import com.example.documate.core.common.generateUuid
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain Use Case encapsulating the complete pipeline for compressing a PDF document:
 * 1. Validating input arguments.
 * 2. Caching original PDF into isolated app-private storage.
 * 3. Calling the on-device native PDF compression engine with the selected preset.
 * 4. Recording conversion history with compression ratio metrics.
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
    ): AppResult<DocumentFile> = withContext(Dispatchers.Default) {
        if (sourcePdf == null) {
            return@withContext AppResult.Error("No PDF selected for compression")
        }

        val cleanName = outputFileName.trim().ifBlank {
            "Compressed_${sourcePdf.name}"
        }

        onProgress(0.05f, "Preparing PDF for compression...")

        try {
            // Stage 1: Copy source PDF to app scratch cache
            val copyResult = documentRepository.copyToAppCache(sourcePdf.uriString, sourcePdf.name)
            val cachedFile: PlatformFile = when (copyResult) {
                is AppResult.Success -> copyResult.data
                is AppResult.Error -> return@withContext AppResult.Error(
                    "Failed to access source PDF: ${copyResult.message}",
                    copyResult.cause
                )
                else -> return@withContext AppResult.Error("Failed to cache source PDF")
            }

            // Stage 2: Compress PDF
            onProgress(0.15f, "Optimizing and compressing PDF streams...")
            val compressResult = pdfRepository.compressPdf(
                sourcePdfFile = cachedFile,
                preset = preset,
                outputFileName = cleanName,
                onProgress = { fraction ->
                    val totalProgress = 0.15f + (0.80f * fraction)
                    val percent = (totalProgress * 100).toInt()
                    onProgress(totalProgress, "Compressing PDF pages ($percent%)...")
                }
            )

            when (compressResult) {
                is AppResult.Success -> {
                    val outputFile = compressResult.data

                    val resultDoc = DocumentFile(
                        id = generateUuid(),
                        name = outputFile.name,
                        uriString = outputFile.path,
                        sizeBytes = outputFile.sizeBytes,
                        mimeType = "application/pdf",
                        extension = "pdf"
                    )

                    // Record history
                    val record = ConversionRecord(
                        id = generateUuid(),
                        tool = DocumentTool.PDF_COMPRESSOR,
                        sourceFileName = sourcePdf.name,
                        outputFileName = outputFile.name,
                        outputPath = outputFile.path,
                        originalSizeBytes = sourcePdf.sizeBytes,
                        outputSizeBytes = outputFile.sizeBytes,
                        timestampMillis = currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    onProgress(1.0f, "Complete!")
                    AppResult.Success(resultDoc)
                }
                is AppResult.Error -> AppResult.Error(compressResult.message, compressResult.cause)
                else -> AppResult.Error("Unexpected compression error")
            }
        } finally {
            documentRepository.clearTemporaryFiles()
        }
    }
}
