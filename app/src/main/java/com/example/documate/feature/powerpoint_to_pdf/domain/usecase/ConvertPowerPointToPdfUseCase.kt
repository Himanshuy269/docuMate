package com.example.documate.feature.powerpoint_to_pdf.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionOptions
import com.example.documate.core.domain.repository.OfficeConversionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Domain Use Case encapsulating the complete pipeline for converting PowerPoint presentations (.pptx, .ppt) to PDF:
 * 1. Validating input arguments (file selection, supported extensions, mandatory user consent).
 * 2. Caching source PowerPoint file into isolated app scratch storage.
 * 3. Calling the Office conversion engine with slides-per-page layout options.
 * 4. Recording conversion history.
 * 5. Cleaning up intermediate scratch files.
 */
class ConvertPowerPointToPdfUseCase(
    private val documentRepository: DocumentRepository,
    private val officeConversionRepository: OfficeConversionRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        sourceFile: DocumentFile?,
        hasConsentedToBackend: Boolean,
        slidesPerPage: Int,
        outputFileName: String,
        backendBaseUrl: String = "http://10.0.2.2:8080",
        onProgress: (progress: Float, stage: String) -> Unit
    ): AppResult<DocumentFile> = withContext(Dispatchers.IO) {
        if (sourceFile == null) {
            return@withContext AppResult.Error("Please select a PowerPoint presentation to convert")
        }

        val ext = sourceFile.extension.lowercase()
        if (ext != "pptx" && ext != "ppt") {
            return@withContext AppResult.Error("Unsupported file type (.$ext). Please select a PowerPoint presentation (.pptx or .ppt).")
        }

        if (!hasConsentedToBackend) {
            return@withContext AppResult.Error("User consent is required before converting document through the service")
        }

        val cleanName = outputFileName.trim().ifBlank {
            "${sourceFile.name.substringBeforeLast('.')}_slides"
        }

        onProgress(0.05f, "Caching PowerPoint presentation...")

        var cachedFile: File? = null
        try {
            // Step 1: Copy to app scratch storage
            val copyResult = documentRepository.copyToAppCache(sourceFile.uriString, sourceFile.name)
            cachedFile = when (copyResult) {
                is AppResult.Success -> copyResult.data
                is AppResult.Error -> {
                    return@withContext AppResult.Error(
                        "Failed to access PowerPoint presentation: ${copyResult.message}",
                        copyResult.cause
                    )
                }
                else -> return@withContext AppResult.Error("Failed to cache PowerPoint presentation")
            }

            // Step 2: Convert to PDF
            onProgress(0.20f, "Rendering slides and presentation graphics to PDF...")
            val options = OfficeConversionOptions(
                slidesPerPage = slidesPerPage.coerceIn(1, 6),
                outputFileName = cleanName
            )

            val conversionResult = officeConversionRepository.convertOfficeDocumentToPdf(
                sourceFile = cachedFile,
                tool = DocumentTool.POWERPOINT_TO_PDF,
                backendBaseUrl = backendBaseUrl,
                options = options,
                onProgress = { fraction ->
                    val progress = 0.20f + (0.75f * fraction)
                    val percent = (progress * 100).toInt()
                    onProgress(progress, "Converting presentation ($percent%)...")
                }
            )

            when (conversionResult) {
                is AppResult.Success -> {
                    val outputFile = conversionResult.data
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
                        tool = DocumentTool.POWERPOINT_TO_PDF,
                        sourceFileName = sourceFile.name,
                        outputFileName = outputFile.name,
                        outputPath = outputFile.absolutePath,
                        originalSizeBytes = sourceFile.sizeBytes,
                        outputSizeBytes = outputFile.length(),
                        timestampMillis = System.currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    onProgress(1.0f, "Complete!")
                    AppResult.Success(resultDoc)
                }
                is AppResult.Error -> {
                    AppResult.Error(conversionResult.message, conversionResult.cause)
                }
                else -> AppResult.Error("Unknown conversion failure")
            }
        } finally {
            try { cachedFile?.delete() } catch (_: Exception) {}
        }
    }
}
