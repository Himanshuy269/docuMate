package com.example.documate.feature.add_images_to_pdf.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.common.currentTimeMillis
import com.example.documate.core.common.generateUuid
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.add_images_to_pdf.presentation.InsertPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain Use Case encapsulating the complete pipeline for inserting images into an existing PDF:
 * 1. Validating input arguments.
 * 2. Caching existing PDF and new images into isolated app-private storage.
 * 3. Calculating 1-based page insertion index.
 * 4. Calling the on-device native PDF manipulation engine.
 * 5. Recording conversion history.
 * 6. Cleaning up intermediate scratch files.
 */
class AddImagesToPdfUseCase(
    private val documentRepository: DocumentRepository,
    private val pdfRepository: PdfRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        existingPdf: DocumentFile?,
        newImages: List<DocumentFile>,
        insertPosition: InsertPosition,
        customPageNumber: Int,
        outputFileName: String,
        onProgress: (progress: Float, stage: String) -> Unit
    ): AppResult<DocumentFile> = withContext(Dispatchers.Default) {
        if (existingPdf == null) {
            return@withContext AppResult.Error("No existing PDF selected")
        }
        if (newImages.isEmpty()) {
            return@withContext AppResult.Error("No images selected to insert")
        }

        val cleanName = outputFileName.trim().ifBlank {
            "Updated_${existingPdf.name}"
        }

        onProgress(0.05f, "Preparing PDF and images...")

        val cachedImages = mutableListOf<PlatformFile>()
        try {
            // Stage 1: Cache existing PDF
            val pdfCopyResult = documentRepository.copyToAppCache(existingPdf.uriString, existingPdf.name)
            val cachedPdf: PlatformFile = when (pdfCopyResult) {
                is AppResult.Success -> pdfCopyResult.data
                is AppResult.Error -> return@withContext AppResult.Error(
                    "Failed to access existing PDF: ${pdfCopyResult.message}",
                    pdfCopyResult.cause
                )
                else -> return@withContext AppResult.Error("Failed to cache PDF")
            }

            // Stage 2: Cache all new images
            val totalImages = newImages.size
            for ((index, image) in newImages.withIndex()) {
                val imageCopyResult = documentRepository.copyToAppCache(image.uriString, image.name)
                when (imageCopyResult) {
                    is AppResult.Success -> cachedImages.add(imageCopyResult.data)
                    is AppResult.Error -> {
                        return@withContext AppResult.Error(
                            "Failed to access image ${image.name}: ${imageCopyResult.message}",
                            imageCopyResult.cause
                        )
                    }
                    else -> {}
                }
                val prepProgress = 0.05f + (0.20f * (index + 1) / totalImages)
                onProgress(prepProgress, "Preparing image ${index + 1} of $totalImages...")
            }

            // Stage 3: Determine target 1-based page insertion index
            val totalPagesResult = pdfRepository.getPdfPageCount(cachedPdf)
            val totalExistingPages = when (totalPagesResult) {
                is AppResult.Success -> totalPagesResult.data
                else -> 1
            }

            val targetPage = when (insertPosition) {
                InsertPosition.AT_BEGINNING -> 1
                InsertPosition.AT_END -> totalExistingPages + 1
                InsertPosition.SPECIFIC_PAGE -> (customPageNumber + 1).coerceIn(1, totalExistingPages + 1)
            }

            // Stage 4: Insert images into PDF
            onProgress(0.25f, "Inserting pages into PDF document...")
            val pdfResult = pdfRepository.addImagesToPdf(
                existingPdfFile = cachedPdf,
                imageFiles = cachedImages,
                insertPositionPage = targetPage,
                outputFileName = cleanName,
                onProgress = { fraction ->
                    val renderProgress = 0.25f + (0.70f * fraction)
                    val percent = (renderProgress * 100).toInt()
                    onProgress(renderProgress, "Inserting and rendering pages ($percent%)...")
                }
            )

            when (pdfResult) {
                is AppResult.Success -> {
                    val outputFile = pdfResult.data
                    val totalOriginalSize = existingPdf.sizeBytes + newImages.sumOf { it.sizeBytes }

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
                        tool = DocumentTool.ADD_IMAGES_TO_PDF,
                        sourceFileName = "${existingPdf.name} + $totalImages images",
                        outputFileName = outputFile.name,
                        outputPath = outputFile.path,
                        originalSizeBytes = totalOriginalSize,
                        outputSizeBytes = outputFile.sizeBytes,
                        timestampMillis = currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    onProgress(1.0f, "Complete!")
                    AppResult.Success(resultDoc)
                }
                is AppResult.Error -> AppResult.Error(pdfResult.message, pdfResult.cause)
                else -> AppResult.Error("Unexpected conversion error")
            }
        } finally {
            documentRepository.clearTemporaryFiles()
        }
    }
}
