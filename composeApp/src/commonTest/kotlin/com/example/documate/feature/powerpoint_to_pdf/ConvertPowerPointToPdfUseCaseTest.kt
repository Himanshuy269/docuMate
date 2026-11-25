package com.example.documate.feature.powerpoint_to_pdf

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionOptions
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.feature.powerpoint_to_pdf.domain.usecase.ConvertPowerPointToPdfUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ConvertPowerPointToPdfUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: FakeDocumentRepository
    private lateinit var fakeOfficeRepo: FakeOfficeConversionRepository
    private lateinit var fakeHistoryRepo: FakeHistoryRepository
    private lateinit var useCase: ConvertPowerPointToPdfUseCase

    private val samplePptx = DocumentFile(
        id = "pptx_doc_1",
        name = "quarterly_review.pptx",
        uriString = "content://docs/quarterly_review.pptx",
        sizeBytes = 512000L, // 500 KB
        mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        extension = "pptx"
    )

    @Before
    fun setup() {
        fakeDocRepo = FakeDocumentRepository(tempFolder)
        fakeOfficeRepo = FakeOfficeConversionRepository(tempFolder)
        fakeHistoryRepo = FakeHistoryRepository()
        useCase = ConvertPowerPointToPdfUseCase(
            documentRepository = fakeDocRepo,
            officeConversionRepository = fakeOfficeRepo,
            historyRepository = fakeHistoryRepo
        )
    }

    // ── Validation tests ──────────────────────────────────────────────────────

    @Test
    fun `null sourceFile returns validation error`() = runBlocking {
        val result = useCase(
            sourceFile = null,
            hasConsentedToBackend = true,
            slidesPerPage = 1,
            outputFileName = "output",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals(
            "Please select a PowerPoint presentation to convert",
            (result as AppResult.Error).message
        )
    }

    @Test
    fun `wrong extension (pdf) returns error`() = runBlocking {
        val wrongFile = samplePptx.copy(
            name = "document.pdf",
            uriString = "content://docs/document.pdf",
            extension = "pdf"
        )

        val result = useCase(
            sourceFile = wrongFile,
            hasConsentedToBackend = true,
            slidesPerPage = 1,
            outputFileName = "output",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Unsupported file type"))
        assertTrue(result.message.contains(".pdf"))
    }

    @Test
    fun `no consent returns error`() = runBlocking {
        val result = useCase(
            sourceFile = samplePptx,
            hasConsentedToBackend = false,
            slidesPerPage = 2,
            outputFileName = "output",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals(
            "User consent is required before converting document through the service",
            (result as AppResult.Error).message
        )
    }

    @Test
    fun `ppt legacy extension also accepted`() = runBlocking {
        val legacyPpt = samplePptx.copy(
            name = "legacy.ppt",
            uriString = "content://docs/legacy.ppt",
            extension = "ppt"
        )

        val result = useCase(
            sourceFile = legacyPpt,
            hasConsentedToBackend = true,
            slidesPerPage = 1,
            outputFileName = "LegacyOutput",
            onProgress = { _, _ -> }
        )

        assertTrue("Expected Success for .ppt file, got $result", result is AppResult.Success)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `valid pptx converts successfully with 1 slide per page and records history`() = runBlocking {
        var lastProgress = 0f
        var lastStage = ""

        val result = useCase(
            sourceFile = samplePptx,
            hasConsentedToBackend = true,
            slidesPerPage = 1,
            outputFileName = "QuarterlyPDF",
            onProgress = { p, stage ->
                lastProgress = p
                lastStage = stage
            }
        )

        assertTrue("Expected Success, got $result", result is AppResult.Success)
        val doc = (result as AppResult.Success).data
        assertEquals("application/pdf", doc.mimeType)
        assertEquals("pdf", doc.extension)
        assertEquals(1.0f, lastProgress, 0.01f)
        assertEquals("Complete!", lastStage)

        // History recorded
        assertEquals(1, fakeHistoryRepo.records.size)
        val record = fakeHistoryRepo.records.first()
        assertEquals(DocumentTool.POWERPOINT_TO_PDF, record.tool)
        assertEquals("quarterly_review.pptx", record.sourceFileName)
        assertEquals(512000L, record.originalSizeBytes)
    }

    @Test
    fun `slidesPerPage 2 is forwarded to repository`() = runBlocking {
        useCase(
            sourceFile = samplePptx,
            hasConsentedToBackend = true,
            slidesPerPage = 2,
            outputFileName = "Output",
            onProgress = { _, _ -> }
        )

        assertEquals(2, fakeOfficeRepo.lastOptions?.slidesPerPage)
    }

    @Test
    fun `slidesPerPage 4 is forwarded to repository`() = runBlocking {
        useCase(
            sourceFile = samplePptx,
            hasConsentedToBackend = true,
            slidesPerPage = 4,
            outputFileName = "Output",
            onProgress = { _, _ -> }
        )

        assertEquals(4, fakeOfficeRepo.lastOptions?.slidesPerPage)
    }

    @Test
    fun `slidesPerPage 6 is forwarded to repository`() = runBlocking {
        useCase(
            sourceFile = samplePptx,
            hasConsentedToBackend = true,
            slidesPerPage = 6,
            outputFileName = "Output",
            onProgress = { _, _ -> }
        )

        assertEquals(6, fakeOfficeRepo.lastOptions?.slidesPerPage)
    }

    @Test
    fun `slidesPerPage above max is clamped to 6`() = runBlocking {
        useCase(
            sourceFile = samplePptx,
            hasConsentedToBackend = true,
            slidesPerPage = 100, // out-of-range
            outputFileName = "Output",
            onProgress = { _, _ -> }
        )

        assertEquals(6, fakeOfficeRepo.lastOptions?.slidesPerPage)
    }

    @Test
    fun `blank outputFileName falls back to source name`() = runBlocking {
        val result = useCase(
            sourceFile = samplePptx,
            hasConsentedToBackend = true,
            slidesPerPage = 1,
            outputFileName = "   ",   // blank
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        val usedName = fakeOfficeRepo.lastOptions?.outputFileName ?: ""
        assertTrue("Expected non-blank fallback name, got '$usedName'", usedName.isNotBlank())
    }

    // ── Failure path ──────────────────────────────────────────────────────────

    @Test
    fun `source file copy failure returns error`() = runBlocking {
        fakeDocRepo.shouldFailCopy = true

        val result = useCase(
            sourceFile = samplePptx,
            hasConsentedToBackend = true,
            slidesPerPage = 1,
            outputFileName = "Output",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Failed to access PowerPoint presentation"))
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    @Test
    fun `conversion engine failure returns error and does not record history`() = runBlocking {
        fakeOfficeRepo.shouldFail = true

        val result = useCase(
            sourceFile = samplePptx,
            hasConsentedToBackend = true,
            slidesPerPage = 2,
            outputFileName = "Output",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Simulated Office conversion error", (result as AppResult.Error).message)
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    // ── Shared Fakes ──────────────────────────────────────────────────────────

    private class FakeDocumentRepository(private val tempFolder: TemporaryFolder) : DocumentRepository {
        var shouldFailCopy = false

        override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> {
            return AppResult.Success(
                DocumentFile("fake", "fake.pdf", uriString, 1000L, "application/pdf", "pdf")
            )
        }

        override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File> {
            if (shouldFailCopy) return AppResult.Error("Disk full or permission denied")
            val file = tempFolder.newFile("temp_${System.currentTimeMillis()}_$preferredName")
            file.writeBytes("dummy-pptx-content".toByteArray())
            return AppResult.Success(file)
        }

        override suspend fun exportFile(sourceFile: File, destinationFileName: String): AppResult<String> {
            val dest = tempFolder.newFile(destinationFileName)
            sourceFile.copyTo(dest, overwrite = true)
            return AppResult.Success(dest.absolutePath)
        }

        override suspend fun copyFileToUri(sourceFile: File, destinationUriString: String): AppResult<Unit> =
            AppResult.Success(Unit)

        override fun getShareableUri(file: File): AppResult<String> =
            AppResult.Success("content://com.example.documate.fileprovider/fake/${file.name}")

        override suspend fun clearTemporaryFiles(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeOfficeConversionRepository(private val tempFolder: TemporaryFolder) : OfficeConversionRepository {
        var shouldFail = false
        var lastOptions: OfficeConversionOptions? = null

        override suspend fun convertOfficeDocumentToPdf(
            sourceFile: File,
            tool: DocumentTool,
            backendBaseUrl: String,
            authToken: String?,
            options: OfficeConversionOptions,
            onProgress: (Float) -> Unit
        ): AppResult<File> {
            if (shouldFail) return AppResult.Error("Simulated Office conversion error")
            lastOptions = options
            onProgress(1.0f)
            val outputDir = tempFolder.newFolder("pptx_pdfs_${System.currentTimeMillis()}")
            val outputFile = File(outputDir, "${sourceFile.nameWithoutExtension}_converted.pdf")
            outputFile.writeText("%PDF-1.4\n%%EOF")
            return AppResult.Success(outputFile)
        }

        override suspend fun checkBackendHealth(backendBaseUrl: String): AppResult<Boolean> =
            AppResult.Success(true)
    }

    private class FakeHistoryRepository : HistoryRepository {
        val records = mutableListOf<ConversionRecord>()

        override fun getRecentConversions(): Flow<List<ConversionRecord>> = flowOf(records)

        override suspend fun addRecord(record: ConversionRecord): AppResult<Unit> {
            records.add(record)
            return AppResult.Success(Unit)
        }

        override suspend fun deleteRecord(id: String): AppResult<Unit> {
            records.removeAll { it.id == id }
            return AppResult.Success(Unit)
        }

        override suspend fun clearAll(): AppResult<Unit> {
            records.clear()
            return AppResult.Success(Unit)
        }
    }
}
