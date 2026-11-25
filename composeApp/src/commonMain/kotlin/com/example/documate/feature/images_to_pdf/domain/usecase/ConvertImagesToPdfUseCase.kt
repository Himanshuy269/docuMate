package com.example.documate.feature.images_to_pdf.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.common.currentTimeMillis
import com.example.documate.core.common.generateUuid
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain Use Case encapsulating the complete pipeline for converting images into a PDF:
 * 1. Validating input arguments.
 * 2. Caching images into isolated app-private storage.
 * 3. Calling the on-device native PDF engine.
 * 4. Recording conversion history.
 * 5. Cleaning up intermediate scratch files.
 */
class ConvertImagesToPdfUseCase(
    private val documentRepository: DocumentRepository,
    private val pdfRepository: PdfRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        selectedImages: List<DocumentFile>,
        pageSize: PdfPageSize,
        orientation: PdfOrientation,
        margin: PdfMargin,
        outputFileName: String,
        onProgress: (progress: Float, stage: String) -> Unit
    ): AppResult<DocumentFile> = withContext(Dispatchers.Default) {
        if (selectedImages.isEmpty()) {
            return@withContext AppResult.Error("No images selected for conversion")
        }

        val cleanName = outputFileName.trim().ifBlank {
            "Images_${currentTimeMillis() / 1000}"
        }

        onProgress(0.05f, "Preparing images...")

        val tempFiles = mutableListOf<PlatformFile>()
        try {
            // Stage 1: Copy selected images to app scratch cache
            val totalImages = selectedImages.size
            for ((index, image) in selectedImages.withIndex()) {
                val copyResult = documentRepository.copyToAppCache(image.uriString, image.name)
                when (copyResult) {
                    is AppResult.Success -> tempFiles.add(copyResult.data)
                    is AppResult.Error -> {
                        return@withContext AppResult.Error(
                            "Failed to access image ${image.name}: ${copyResult.message}",
                            copyResult.cause
                        )
                    }
                    else -> {}
                }
                val prepProgress = 0.05f + (0.25f * (index + 1) / totalImages)
                onProgress(prepProgress, "Preparing image ${index + 1} of $totalImages...")
            }

            // Stage 2: Render PDF pages
            onProgress(0.30f, "Generating PDF document...")
            val pdfResult = pdfRepository.createPdfFromImages(
                imageFiles = tempFiles,
                pageSize = pageSize,
                orientation = orientation,
                margin = margin,
                outputFileName = cleanName,
                onProgress = { fraction ->
                    val renderProgress = 0.30f + (0.65f * fraction)
                    val percent = (renderProgress * 100).toInt()
                    onProgress(renderProgress, "Rendering PDF pages ($percent%)...")
                }
            )

            when (pdfResult) {
                is AppResult.Success -> {
                    val outputFile = pdfResult.data
                    val totalOriginalSize = selectedImages.sumOf { it.sizeBytes }

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
                        tool = DocumentTool.IMAGES_TO_PDF,
                        sourceFileName = "$totalImages images",
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
