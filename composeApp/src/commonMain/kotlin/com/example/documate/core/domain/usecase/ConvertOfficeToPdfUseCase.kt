package com.example.documate.core.domain.usecase

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
import com.example.documate.core.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain Use Case encapsulating the complete pipeline for converting Office files (Word, Excel, PowerPoint) to PDF:
 * 1. Validating file selection and file extension.
 * 2. Enforcing explicit user consent before sending documents to the conversion backend.
 * 3. Caching document into isolated sandbox storage.
 * 4. Uploading to LibreOffice conversion backend and streaming PDF result.
 * 5. Recording conversion in History.
 * 6. Cleaning up scratch cache.
 */
class ConvertOfficeToPdfUseCase(
    private val documentRepository: DocumentRepository,
    private val officeConversionRepository: OfficeConversionRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        sourceDocument: DocumentFile,
        tool: DocumentTool,
        hasUserConsented: Boolean,
        customOutputName: String? = null,
        onProgress: (progress: Float, stage: String) -> Unit = { _, _ -> }
    ): AppResult<DocumentFile> = withContext(Dispatchers.Default) {
        // Validation 1: User consent check
        if (!hasUserConsented) {
            return@withContext AppResult.Error(
                "Consent required: You must explicitly approve uploading this document to the conversion server."
            )
        }

        // Validation 2: Supported tool and extension check
        val ext = sourceDocument.extension.lowercase()
        if (!tool.supportedExtensions.contains(ext)) {
            return@withContext AppResult.Error(
                "Unsupported file extension '.$ext' for ${tool.title}. Supported types: ${tool.supportedExtensions.joinToString { ".$it" }}"
            )
        }

        onProgress(0.05f, "Preparing document...")

        // Stage 1: Copy to app sandbox cache
        val cachedFile: PlatformFile = when (val copyResult = documentRepository.copyToAppCache(sourceDocument.uriString, sourceDocument.name)) {
            is AppResult.Success -> copyResult.data
            is AppResult.Error -> return@withContext AppResult.Error("Failed to access document: ${copyResult.message}", copyResult.cause)
            else -> return@withContext AppResult.Error("Failed to cache document")
        }

        try {
            val backendUrl = settingsRepository.getSettingsSync().backendServiceUrl

            onProgress(0.20f, "Connecting to LibreOffice conversion service...")
            val convertResult = officeConversionRepository.convertOfficeDocumentToPdf(
                sourceFile = cachedFile,
                tool = tool,
                backendBaseUrl = backendUrl,
                onProgress = { p ->
                    val totalProgress = 0.20f + (p * 0.75f)
                    val percent = (totalProgress * 100).toInt()
                    val stageName = when {
                        p < 0.35f -> "Uploading document to server..."
                        p < 0.75f -> "Converting document via LibreOffice ($percent%)..."
                        else -> "Downloading converted PDF ($percent%)..."
                    }
                    onProgress(totalProgress, stageName)
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
                        tool = tool,
                        sourceFileName = sourceDocument.name,
                        outputFileName = finalName,
                        outputPath = outputFile.path,
                        originalSizeBytes = sourceDocument.sizeBytes,
                        outputSizeBytes = outputFile.sizeBytes,
                        timestampMillis = currentTimeMillis()
                    )
                    historyRepository.addRecord(record)

                    onProgress(1.0f, "Complete!")
                    AppResult.Success(resultDoc)
                }
                is AppResult.Error -> {
                    AppResult.Error(convertResult.message, convertResult.cause)
                }
                else -> AppResult.Error("Unknown conversion error")
            }
        } finally {
            documentRepository.clearTemporaryFiles()
        }
    }
}
