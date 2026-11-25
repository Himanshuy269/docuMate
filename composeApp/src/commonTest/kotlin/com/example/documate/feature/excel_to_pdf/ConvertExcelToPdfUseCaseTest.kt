package com.example.documate.feature.excel_to_pdf

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionOptions
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.feature.excel_to_pdf.domain.usecase.ConvertExcelToPdfUseCase
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

class ConvertExcelToPdfUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: FakeDocumentRepository
    private lateinit var fakeOfficeRepo: FakeOfficeConversionRepository
    private lateinit var fakeHistoryRepo: FakeHistoryRepository
    private lateinit var useCase: ConvertExcelToPdfUseCase

    private val sampleXlsx = DocumentFile(
        id = "excel_doc_1",
        name = "budget.xlsx",
        uriString = "content://docs/budget.xlsx",
        sizeBytes = 102400L, // 100 KB
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        extension = "xlsx"
    )

    @Before
    fun setup() {
        fakeDocRepo = FakeDocumentRepository(tempFolder)
        fakeOfficeRepo = FakeOfficeConversionRepository(tempFolder)
        fakeHistoryRepo = FakeHistoryRepository()
        useCase = ConvertExcelToPdfUseCase(
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
            fitToPageWidth = false,
            outputFileName = "output",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Please select an Excel workbook to convert", (result as AppResult.Error).message)
    }

    @Test
    fun `wrong extension (docx) returns error`() = runBlocking {
        val wrongFile = sampleXlsx.copy(
            name = "report.docx",
            uriString = "content://docs/report.docx",
            extension = "docx"
        )

        val result = useCase(
            sourceFile = wrongFile,
            hasConsentedToBackend = true,
            fitToPageWidth = false,
            outputFileName = "output",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Unsupported file type"))
        assertTrue(result.message.contains(".docx"))
    }

    @Test
    fun `no consent returns error`() = runBlocking {
        val result = useCase(
            sourceFile = sampleXlsx,
            hasConsentedToBackend = false,
            fitToPageWidth = true,
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
    fun `xls legacy extension also accepted`() = runBlocking {
        val legacyXls = sampleXlsx.copy(
            name = "legacy.xls",
            uriString = "content://docs/legacy.xls",
            extension = "xls"
        )

        val result = useCase(
            sourceFile = legacyXls,
            hasConsentedToBackend = true,
            fitToPageWidth = false,
            outputFileName = "LegacyOutput",
            onProgress = { _, _ -> }
        )

        assertTrue("Expected Success for .xls file, got $result", result is AppResult.Success)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `valid xlsx converts successfully and records history`() = runBlocking {
        var lastProgress = 0f
        var lastStage = ""

        val result = useCase(
            sourceFile = sampleXlsx,
            hasConsentedToBackend = true,
            fitToPageWidth = false,
            outputFileName = "BudgetPDF",
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
        assertEquals(DocumentTool.EXCEL_TO_PDF, record.tool)
        assertEquals("budget.xlsx", record.sourceFileName)
        assertEquals(102400L, record.originalSizeBytes)
    }

    @Test
    fun `fitToPageWidth true is forwarded to repository`() = runBlocking {
        useCase(
            sourceFile = sampleXlsx,
            hasConsentedToBackend = true,
            fitToPageWidth = true,
            outputFileName = "Output",
            onProgress = { _, _ -> }
        )

        assertTrue(fakeOfficeRepo.lastOptions?.fitToPageWidth == true)
    }

    @Test
    fun `fitToPageWidth false is forwarded to repository`() = runBlocking {
        useCase(
            sourceFile = sampleXlsx,
            hasConsentedToBackend = true,
            fitToPageWidth = false,
            outputFileName = "Output",
            onProgress = { _, _ -> }
        )

        assertTrue(fakeOfficeRepo.lastOptions?.fitToPageWidth == false)
    }

    @Test
    fun `blank outputFileName falls back to source name`() = runBlocking {
        val result = useCase(
            sourceFile = sampleXlsx,
            hasConsentedToBackend = true,
            fitToPageWidth = false,
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
            sourceFile = sampleXlsx,
            hasConsentedToBackend = true,
            fitToPageWidth = false,
            outputFileName = "Output",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Failed to access Excel workbook"))
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    @Test
    fun `conversion engine failure returns error and does not record history`() = runBlocking {
        fakeOfficeRepo.shouldFail = true

        val result = useCase(
            sourceFile = sampleXlsx,
            hasConsentedToBackend = true,
            fitToPageWidth = false,
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
            file.writeBytes("dummy-excel-content".toByteArray())
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
            val outputDir = tempFolder.newFolder("excel_pdfs_${System.currentTimeMillis()}")
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
