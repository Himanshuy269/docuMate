package com.example.documate.feature.add_images_to_pdf.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.add_images_to_pdf.presentation.InsertPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Domain Use Case encapsulating the complete pipeline for inserting images into an existing PDF:
 * 1. Validating inputs (source PDF existence, non-empty image list, sanitizing output file name).
 * 2. Caching source PDF and images into isolated app scratch storage.
 * 3. Calling the on-device native PDF insertion engine.
 * 4. Recording conversion history.
 * 5. Cleaning up intermediate scratch files.
 */
class AddImagesToPdfUseCase(
    private val documentRepository: DocumentRepository,
    private val pdfRepository: PdfRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        sourcePdf: DocumentFile?,
        selectedImages: List<DocumentFile>,
        insertPosition: InsertPosition,
        specificPageNumber: Int,
        outputFileName: String,
        onProgress: (progress: Float, stage: String) -> Unit
    ): AppResult<DocumentFile> = withContext(Dispatchers.IO) {
        if (sourcePdf == null) {
            return@withContext AppResult.Error("Please select a source PDF document")
        }
        if (selectedImages.isEmpty()) {
            return@withContext AppResult.Error("Please select at least one image to insert")
        }

        val cleanName = outputFileName.trim().ifBlank {
            "${sourcePdf.name.substringBeforeLast('.')}_modified"
        }

        onProgress(0.05f, "Preparing source PDF...")

        val tempFiles = mutableListOf<File>()
        try {
            // Stage 1: Copy source PDF to scratch cache
            val pdfCopyResult = documentRepository.copyToAppCache(sourcePdf.uriString, sourcePdf.name)
            val cachedPdf = when (pdfCopyResult) {
                is AppResult.Success -> {
                    tempFiles.add(pdfCopyResult.data)
                    pdfCopyResult.data
                }
                is AppResult.Error -> {
                    return@withContext AppResult.Error(
                        "Failed to access source PDF: ${pdfCopyResult.message}",
                        pdfCopyResult.cause
                    )
                }
                else -> return@withContext AppResult.Error("Failed to cache source PDF")
            }

            // Stage 2: Copy selected images to scratch cache
            val totalImages = selectedImages.size
            val cachedImages = mutableListOf<File>()
            for ((index, image) in selectedImages.withIndex()) {
                val copyResult = documentRepository.copyToAppCache(image.uriString, image.name)
                when (copyResult) {
                    is AppResult.Success -> {
                        tempFiles.add(copyResult.data)
                        cachedImages.add(copyResult.data)
                    }
                    is AppResult.Error -> {
                        return@withContext AppResult.Error(
                            "Failed to access image ${image.name}: ${copyResult.message}",
                            copyResult.cause
                        )
                    }
                    else -> {}
                }
                val prepProgress = 0.05f + (0.20f * (index + 1) / totalImages)
                onProgress(prepProgress, "Preparing image ${index + 1} of $totalImages...")
            }

            // Stage 3: Determine target insert position page index
            val insertPage = when (insertPosition) {
                InsertPosition.AT_BEGINNING -> 0
                InsertPosition.AT_END -> -1
                InsertPosition.SPECIFIC_PAGE -> specificPageNumber.coerceAtLeast(1)
            }

            // Stage 4: Run native insertion engine
            onProgress(0.30f, "Inserting images into PDF...")
            val insertResult = pdfRepository.addImagesToPdf(
                existingPdfFile = cachedPdf,
                imageFiles = cachedImages,
                insertPositionPage = insertPage,
                outputFileName = cleanName,
                onProgress = { fraction ->
                    val renderProgress = 0.30f + (0.60f * fraction)
                    val percent = (renderProgress * 100).toInt()
                    onProgress(renderProgress, "Processing PDF pages ($percent%)...")
                }
            )

            when (insertResult) {
                is AppResult.Success -> {
                    val outputFile = insertResult.data
                    val totalOriginalSize = sourcePdf.sizeBytes + selectedImages.sumOf { it.sizeBytes }

                    val resultDoc = DocumentFile(
                        id = UUID.randomUUID().toString(),
                        name = outputFile.name,
                        uriString = outputFile.toURI().toString(),
                        sizeBytes = outputFile.length(),
                        mimeType = "application/pdf",
                        extension = "pdf"
                    )

                    onProgress(0.95f, "Finalizing and recording history...")
                    val record = ConversionRecord(
                        id = UUID.randomUUID().toString(),
                        tool = DocumentTool.ADD_IMAGES_TO_PDF,
                        sourceFileName = "${sourcePdf.name} + ${selectedImages.size} image(s)",
                        outputFileName = outputFile.name,
                        outputPath = outputFile.absolutePath,
                        originalSizeBytes = totalOriginalSize,
                        outputSizeBytes = outputFile.length(),
                        timestampMillis = System.currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    onProgress(1.0f, "Complete!")
                    AppResult.Success(resultDoc)
                }
                is AppResult.Error -> AppResult.Error(insertResult.message, insertResult.cause)
                else -> AppResult.Error("Unknown error occurred during PDF generation")
            }
        } finally {
            // Clean up temporary scratch files
            for (file in tempFiles) {
                try {
                    if (file.exists()) file.delete()
                } catch (_: Exception) {}
            }
        }
    }
}
