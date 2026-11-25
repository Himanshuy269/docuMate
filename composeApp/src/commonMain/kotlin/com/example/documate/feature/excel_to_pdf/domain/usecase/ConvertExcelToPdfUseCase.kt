package com.example.documate.feature.excel_to_pdf.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.common.currentTimeMillis
import com.example.documate.core.common.generateUuid
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionOptions
import com.example.documate.core.domain.repository.OfficeConversionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain Use Case encapsulating the complete pipeline for converting Excel spreadsheets (.xlsx, .xls) to PDF:
 * 1. Validating input arguments (file selection, supported extensions, mandatory user consent).
 * 2. Caching source Excel spreadsheet into isolated app scratch storage.
 * 3. Calling the Office conversion engine with fit-to-page options.
 * 4. Recording conversion history.
 * 5. Cleaning up intermediate scratch files.
 */
class ConvertExcelToPdfUseCase(
    private val documentRepository: DocumentRepository,
    private val officeConversionRepository: OfficeConversionRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        sourceFile: DocumentFile?,
        hasUserConsented: Boolean,
        backendBaseUrl: String,
        fitToPageWidth: Boolean = true,
        customOutputName: String? = null,
        onProgress: (progress: Float, stage: String) -> Unit = { _, _ -> }
    ): AppResult<DocumentFile> = withContext(Dispatchers.Default) {
        if (sourceFile == null) {
            return@withContext AppResult.Error("No Excel spreadsheet selected")
        }

        if (!hasUserConsented) {
            return@withContext AppResult.Error("Explicit consent required before transmitting spreadsheet to conversion service")
        }

        val ext = sourceFile.extension.lowercase()
        if (ext != "xlsx" && ext != "xls") {
            return@withContext AppResult.Error("Unsupported file extension: .$ext. Only .xlsx and .xls are supported.")
        }

        onProgress(0.05f, "Preparing Excel spreadsheet...")

        try {
            // Stage 1: Cache document
            val copyResult = documentRepository.copyToAppCache(sourceFile.uriString, sourceFile.name)
            val cachedFile: PlatformFile = when (copyResult) {
                is AppResult.Success -> copyResult.data
                is AppResult.Error -> return@withContext AppResult.Error(
                    "Failed to access spreadsheet: ${copyResult.message}",
                    copyResult.cause
                )
                else -> return@withContext AppResult.Error("Failed to cache spreadsheet")
            }

            // Stage 2: Convert via Office conversion repository
            onProgress(0.20f, "Converting Excel spreadsheet to PDF...")
            val convertResult = officeConversionRepository.convertOfficeDocumentToPdf(
                sourceFile = cachedFile,
                tool = DocumentTool.EXCEL_TO_PDF,
                backendBaseUrl = backendBaseUrl,
                options = OfficeConversionOptions(fitToPageWidth = fitToPageWidth),
                onProgress = { p ->
                    val totalProgress = 0.20f + (p * 0.75f)
                    val percent = (totalProgress * 100).toInt()
                    val stage = when {
                        p < 0.35f -> "Uploading to LibreOffice engine..."
                        p < 0.75f -> "Rendering spreadsheet tables ($percent%)..."
                        else -> "Downloading converted PDF ($percent%)..."
                    }
                    onProgress(totalProgress, stage)
                }
            )

            when (convertResult) {
                is AppResult.Success -> {
                    val outputFile = convertResult.data
                    val finalName = if (!customOutputName.isNullOrBlank()) {
                        if (customOutputName.endsWith(".pdf", ignoreCase = true)) customOutputName else "$customOutputName.pdf"
                    } else {
                        outputFile.name
                    }

                    val resultDoc = DocumentFile(
                        id = generateUuid(),
                        name = finalName,
                        uriString = outputFile.path,
                        sizeBytes = outputFile.sizeBytes,
                        mimeType = "application/pdf",
                        extension = "pdf"
                    )

                    // Record history
                    val record = ConversionRecord(
                        id = generateUuid(),
                        tool = DocumentTool.EXCEL_TO_PDF,
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
