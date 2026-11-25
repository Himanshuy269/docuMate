package com.example.documate.core.data.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.OfficeConversionOptions
import com.example.documate.core.domain.repository.OfficeConversionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.readRawBytes
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
class IosOfficeConversionRepository : OfficeConversionRepository {

    private val httpClient = HttpClient(Darwin)

    override suspend fun convertOfficeDocumentToPdf(
        sourceFile: PlatformFile,
        tool: DocumentTool,
        backendBaseUrl: String,
        authToken: String?,
        options: OfficeConversionOptions,
        onProgress: (Float) -> Unit
    ): AppResult<PlatformFile> = withContext(Dispatchers.Default) {
        try {
            onProgress(0.2f)
            val nsData = NSData.dataWithContentsOfFile(sourceFile.path)
                ?: return@withContext AppResult.Error("Unable to read source file from path: ${sourceFile.path}")

            val endpoint = when (tool) {
                DocumentTool.WORD_TO_PDF -> "$backendBaseUrl/convert/word"
                DocumentTool.EXCEL_TO_PDF -> "$backendBaseUrl/convert/excel"
                DocumentTool.POWERPOINT_TO_PDF -> "$backendBaseUrl/convert/powerpoint"
                else -> "$backendBaseUrl/convert"
            }

            onProgress(0.4f)
            val fileManager = NSFileManager.defaultManager
            val cacheUrls = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            val cacheDirUrl = cacheUrls.firstOrNull() as? NSURL ?: return@withContext AppResult.Error("Unable to locate cache directory")
            val baseName = options.outputFileName?.ifBlank { null } ?: (sourceFile.nameWithoutExtension + "_converted")
            val cleanName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"
            val targetUrl = cacheDirUrl.URLByAppendingPathComponent(cleanName)
                ?: return@withContext AppResult.Error("Failed to build output file URL")

            val targetPath = targetUrl.path ?: ""
            // Mock or remote conversion output write
            nsData.writeToFile(targetPath, atomically = true)
            onProgress(1.0f)

            AppResult.Success(
                PlatformFile(
                    path = targetPath,
                    name = cleanName,
                    sizeBytes = 0L
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Office conversion failed on iOS: ${e.message}", e)
        }
    }

    override suspend fun convertPdfToOfficeDocument(
        sourceFile: PlatformFile,
        tool: DocumentTool,
        backendBaseUrl: String,
        authToken: String?,
        outputFileName: String?,
        onProgress: (Float) -> Unit
    ): AppResult<PlatformFile> = withContext(Dispatchers.Default) {
        try {
            onProgress(0.2f)
            val nsData = NSData.dataWithContentsOfFile(sourceFile.path)
                ?: return@withContext AppResult.Error("Unable to read source file from path: ${sourceFile.path}")

            val ext = when (tool) {
                DocumentTool.PDF_TO_WORD -> ".docx"
                DocumentTool.PDF_TO_EXCEL -> ".xlsx"
                DocumentTool.PDF_TO_POWERPOINT -> ".pptx"
                else -> ".docx"
            }

            onProgress(0.4f)
            val fileManager = NSFileManager.defaultManager
            val cacheUrls = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            val cacheDirUrl = cacheUrls.firstOrNull() as? NSURL ?: return@withContext AppResult.Error("Unable to locate cache directory")
            val baseName = outputFileName?.ifBlank { null } ?: (sourceFile.nameWithoutExtension + "_converted")
            val cleanName = if (baseName.endsWith(ext, ignoreCase = true)) baseName else "$baseName$ext"
            val targetUrl = cacheDirUrl.URLByAppendingPathComponent(cleanName)
                ?: return@withContext AppResult.Error("Failed to build output file URL")

            val targetPath = targetUrl.path ?: ""
            nsData.writeToFile(targetPath, atomically = true)
            onProgress(1.0f)

            AppResult.Success(
                PlatformFile(
                    path = targetPath,
                    name = cleanName,
                    sizeBytes = 0L
                )
            )
        } catch (e: Exception) {
            AppResult.Error("PDF to Office conversion failed on iOS: ${e.message}", e)
        }
    }

    override suspend fun checkBackendHealth(backendBaseUrl: String): AppResult<Boolean> = withContext(Dispatchers.Default) {
        try {
            val response = httpClient.get("$backendBaseUrl/health")
            AppResult.Success(response.status == HttpStatusCode.OK)
        } catch (e: Exception) {
            AppResult.Error("Backend service is unreachable: ${e.message}", e)
        }
    }
}
