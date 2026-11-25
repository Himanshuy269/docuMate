package com.example.documate.feature.pdf_to_excel.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.common.currentTimeMillis
import com.example.documate.core.common.generateUuid
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain Use Case encapsulating the complete pipeline for converting PDF documents to Excel (.xlsx):
 * 1. Validating input arguments (file selection, supported extensions, mandatory user consent).
 * 2. Caching source PDF document into isolated app scratch storage.
 * 3. Calling the Office conversion engine with target format xlsx.
 * 4. Recording conversion history.
 * 5. Cleaning up intermediate scratch files.
 */
class ConvertPdfToExcelUseCase(
    private val documentRepository: DocumentRepository,
    private val officeConversionRepository: OfficeConversionRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        sourceFile: DocumentFile?,
        hasUserConsented: Boolean,
        backendBaseUrl: String,
        customOutputName: String? = null,
        onProgress: (progress: Float, stage: String) -> Unit = { _, _ -> }
    ): AppResult<DocumentFile> = withContext(Dispatchers.Default) {
        if (sourceFile == null) {
            return@withContext AppResult.Error("No PDF document selected")
        }

        if (!hasUserConsented) {
            return@withContext AppResult.Error("Explicit consent required before transmitting document to conversion service")
        }

        val ext = sourceFile.extension.lowercase()
        if (ext != "pdf") {
            return@withContext AppResult.Error("Unsupported file extension: .$ext. Only .pdf is supported.")
        }

        onProgress(0.05f, "Preparing PDF document...")

        try {
            // Stage 1: Cache document
            val copyResult = documentRepository.copyToAppCache(sourceFile.uriString, sourceFile.name)
            val cachedFile: PlatformFile = when (copyResult) {
                is AppResult.Success -> copyResult.data
                is AppResult.Error -> return@withContext AppResult.Error(
                    "Failed to access document: ${copyResult.message}",
                    copyResult.cause
                )
                else -> return@withContext AppResult.Error("Failed to cache document")
            }

            // Stage 2: Convert via Office conversion repository
            onProgress(0.20f, "Converting PDF to Excel spreadsheet...")
            val convertResult = officeConversionRepository.convertPdfToOfficeDocument(
                sourceFile = cachedFile,
                tool = DocumentTool.PDF_TO_EXCEL,
                backendBaseUrl = backendBaseUrl,
                outputFileName = customOutputName,
                onProgress = { p ->
                    val totalProgress = 0.20f + (p * 0.75f)
                    val percent = (totalProgress * 100).toInt()
                    val stage = when {
                        p < 0.35f -> "Detecting tabular data in PDF..."
                        p < 0.75f -> "Generating Excel rows and columns ($percent%)..."
                        else -> "Finalizing XLSX spreadsheet ($percent%)..."
                    }
                    onProgress(totalProgress, stage)
                }
            )

            when (convertResult) {
                is AppResult.Success -> {
                    val outputFile = convertResult.data
                    val finalName = if (!customOutputName.isNullOrBlank()) {
                        if (customOutputName.endsWith(".xlsx", ignoreCase = true)) customOutputName else "$customOutputName.xlsx"
                    } else {
                        outputFile.name
                    }

                    val resultDoc = DocumentFile(
                        id = generateUuid(),
                        name = finalName,
                        uriString = outputFile.path,
                        sizeBytes = outputFile.sizeBytes,
                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        extension = "xlsx"
                    )

                    // Record history
                    val record = ConversionRecord(
                        id = generateUuid(),
                        tool = DocumentTool.PDF_TO_EXCEL,
                        sourceFileName = sourceFile.name,
                        outputFileName = finalName,
                        outputPath = outputFile.path,
                        originalSizeBytes = sourceFile.sizeBytes,
                        outputSizeBytes = outputFile.sizeBytes,
                        timestampMillis = currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    onProgress(1.0f, "Complete!")
                    AppResult.Success(resultDoc)
                }
                is AppResult.Error -> AppResult.Error(convertResult.message, convertResult.cause)
                else -> AppResult.Error("Unexpected conversion error")
            }
        } finally {
            documentRepository.clearTemporaryFiles()
        }
    }
}
