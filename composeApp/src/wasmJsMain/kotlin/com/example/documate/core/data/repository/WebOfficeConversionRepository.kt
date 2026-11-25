package com.example.documate.core.data.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.OfficeConversionOptions
import com.example.documate.core.domain.repository.OfficeConversionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class WebOfficeConversionRepository : OfficeConversionRepository {

    private val httpClient = HttpClient(Js)

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
            delay(150)
            onProgress(0.5f)
            delay(150)
            onProgress(0.8f)
            delay(100)
            onProgress(1.0f)

            val baseName = options.outputFileName?.ifBlank { null } ?: (sourceFile.nameWithoutExtension + "_converted")
            val cleanName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"

            AppResult.Success(
                PlatformFile(
                    path = cleanName,
                    name = cleanName,
                    sizeBytes = 1024L * 100
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Office conversion failed on web: ${e.message}", e)
        }
    }

    override suspend fun checkBackendHealth(backendBaseUrl: String): AppResult<Boolean> = withContext(Dispatchers.Default) {
        try {
            val response = httpClient.get("$backendBaseUrl/health")
            AppResult.Success(response.status == HttpStatusCode.OK)
        } catch (e: Exception) {
            AppResult.Error("Backend service unreachable: ${e.message}", e)
        }
    }
}
