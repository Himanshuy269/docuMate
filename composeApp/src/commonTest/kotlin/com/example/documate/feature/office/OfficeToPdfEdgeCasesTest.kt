package com.example.documate.feature.office

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.UserSettings
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionOptions
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.core.domain.repository.SettingsRepository
import com.example.documate.core.domain.usecase.ConvertOfficeToPdfUseCase
import com.example.documate.feature.excel_to_pdf.domain.usecase.ConvertExcelToPdfUseCase
import com.example.documate.feature.powerpoint_to_pdf.domain.usecase.ConvertPowerPointToPdfUseCase
import com.example.documate.feature.word_to_pdf.domain.usecase.ConvertWordToPdfUseCase
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

class OfficeToPdfEdgeCasesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: MockDocumentRepository
    private lateinit var fakeOfficeRepo: MockOfficeConversionRepository
    private lateinit var fakeHistoryRepo: MockHistoryRepository
    private lateinit var fakeSettingsRepo: MockSettingsRepository

    private lateinit var wordUseCase: ConvertWordToPdfUseCase
    private lateinit var excelUseCase: ConvertExcelToPdfUseCase
    private lateinit var pptUseCase: ConvertPowerPointToPdfUseCase
    private lateinit var genericOfficeUseCase: ConvertOfficeToPdfUseCase

    @Before
    fun setup() {
        fakeDocRepo = MockDocumentRepository(tempFolder)
        fakeOfficeRepo = MockOfficeConversionRepository(tempFolder)
        fakeHistoryRepo = MockHistoryRepository()
        fakeSettingsRepo = MockSettingsRepository()

        wordUseCase = ConvertWordToPdfUseCase(fakeDocRepo, fakeOfficeRepo, fakeHistoryRepo)
        excelUseCase = ConvertExcelToPdfUseCase(fakeDocRepo, fakeOfficeRepo, fakeHistoryRepo)
        pptUseCase = ConvertPowerPointToPdfUseCase(fakeDocRepo, fakeOfficeRepo, fakeHistoryRepo)
        genericOfficeUseCase = ConvertOfficeToPdfUseCase(fakeDocRepo, fakeOfficeRepo, fakeHistoryRepo, fakeSettingsRepo)
    }

    // --- WORD TO PDF EDGE CASES ---

    @Test
    fun `word conversion without consent returns error`() = runBlocking {
        val wordDoc = DocumentFile("w1", "resume.docx", "uri://w1", 1000L, extension = "docx")
        val result = wordUseCase(
            sourceFile = wordDoc,
            hasConsentedToBackend = false,
            preserveHyperlinks = true,
            outputFileName = "resume",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("User consent is required"))
    }

    @Test
    fun `word conversion with invalid extension returns error`() = runBlocking {
        val txtDoc = DocumentFile("w2", "notes.txt", "uri://w2", 500L, extension = "txt")
        val result = wordUseCase(
            sourceFile = txtDoc,
            hasConsentedToBackend = true,
            preserveHyperlinks = true,
            outputFileName = "notes",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Unsupported file type (.txt)"))
    }

    @Test
    fun `word conversion with doc extension succeeds`() = runBlocking {
        val legacyDoc = DocumentFile("w3", "old_spec.doc", "uri://w3", 1500L, extension = "doc")
        val result = wordUseCase(
            sourceFile = legacyDoc,
            hasConsentedToBackend = true,
            preserveHyperlinks = true,
            outputFileName = "old_spec",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals("old_spec.pdf", (result as AppResult.Success).data.name)
        assertEquals(1, fakeHistoryRepo.records.size)
        assertEquals(DocumentTool.WORD_TO_PDF, fakeHistoryRepo.records.first().tool)
    }

    // --- EXCEL TO PDF EDGE CASES ---

    @Test
    fun `excel conversion without consent returns error`() = runBlocking {
        val xlsxDoc = DocumentFile("x1", "budget.xlsx", "uri://x1", 2000L, extension = "xlsx")
        val result = excelUseCase(
            sourceFile = xlsxDoc,
            hasConsentedToBackend = false,
            fitToPageWidth = true,
            outputFileName = "budget",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("User consent is required"))
    }

    @Test
    fun `excel conversion with csv extension returns error`() = runBlocking {
        val csvDoc = DocumentFile("x2", "data.csv", "uri://x2", 800L, extension = "csv")
        val result = excelUseCase(
            sourceFile = csvDoc,
            hasConsentedToBackend = true,
            fitToPageWidth = false,
            outputFileName = "data",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Unsupported file type (.csv)"))
    }

    @Test
    fun `excel conversion with xls extension succeeds`() = runBlocking {
        val legacyXls = DocumentFile("x3", "sheet.xls", "uri://x3", 1200L, extension = "xls")
        val result = excelUseCase(
            sourceFile = legacyXls,
            hasConsentedToBackend = true,
            fitToPageWidth = true,
            outputFileName = "sheet",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals("sheet.pdf", (result as AppResult.Success).data.name)
    }

    // --- POWERPOINT TO PDF EDGE CASES ---

    @Test
    fun `powerpoint conversion without consent returns error`() = runBlocking {
        val pptxDoc = DocumentFile("p1", "deck.pptx", "uri://p1", 5000L, extension = "pptx")
        val result = pptUseCase(
            sourceFile = pptxDoc,
            hasConsentedToBackend = false,
            slidesPerPage = 2,
            outputFileName = "deck",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("User consent is required"))
    }

    @Test
    fun `powerpoint conversion with invalid extension returns error`() = runBlocking {
        val pdfDoc = DocumentFile("p2", "deck.pdf", "uri://p2", 5000L, extension = "pdf")
        val result = pptUseCase(
            sourceFile = pdfDoc,
            hasConsentedToBackend = true,
            slidesPerPage = 1,
            outputFileName = "deck",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Unsupported file type (.pdf)"))
    }

    @Test
    fun `powerpoint conversion with ppt extension succeeds`() = runBlocking {
        val legacyPpt = DocumentFile("p3", "slides.ppt", "uri://p3", 4000L, extension = "ppt")
        val result = pptUseCase(
            sourceFile = legacyPpt,
            hasConsentedToBackend = true,
            slidesPerPage = 4,
            outputFileName = "slides",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals("slides.pdf", (result as AppResult.Success).data.name)
    }

    // --- GENERIC CONVERTER EDGE CASES ---

    @Test
    fun `generic converter rejects unsupported tool extensions`() = runBlocking {
        val imageDoc = DocumentFile("g1", "photo.png", "uri://g1", 1000L, extension = "png")
        val result = genericOfficeUseCase(
            sourceDocument = imageDoc,
            tool = DocumentTool.WORD_TO_PDF,
            hasUserConsented = true
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Unsupported file extension '.png'"))
    }

    // --- Mocks ---

    private class MockDocumentRepository(private val tempFolder: TemporaryFolder) : DocumentRepository {
        override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> = AppResult.Success(
            DocumentFile("id", "doc.docx", uriString, 1000)
        )
        override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File> {
            val file = tempFolder.newFile("temp_${System.currentTimeMillis()}_$preferredName")
            file.writeBytes("dummy-office-bytes".toByteArray())
            return AppResult.Success(file)
        }
        override suspend fun exportFile(sourceFile: File, destinationFileName: String): AppResult<String> = AppResult.Success("")
        override suspend fun copyFileToUri(sourceFile: File, destinationUriString: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun getShareableUri(file: File): AppResult<String> = AppResult.Success("content://share/${file.name}")
        override suspend fun clearTemporaryFiles(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class MockOfficeConversionRepository(private val tempFolder: TemporaryFolder) : OfficeConversionRepository {
        override suspend fun convertOfficeDocumentToPdf(
            sourceFile: File,
            tool: DocumentTool,
            backendBaseUrl: String,
            authToken: String?,
            options: OfficeConversionOptions,
            onProgress: (Float) -> Unit
        ): AppResult<File> {
            val cleanName = options.outputFileName?.let {
                if (it.endsWith(".pdf")) it else "$it.pdf"
            } ?: "${sourceFile.nameWithoutExtension}.pdf"
            val file = tempFolder.newFile(cleanName)
            file.writeBytes("%PDF-1.4\nOfficeDoc\n%%EOF".toByteArray())
            onProgress(1.0f)
            return AppResult.Success(file)
        }

        override suspend fun checkBackendHealth(backendBaseUrl: String): AppResult<Boolean> = AppResult.Success(true)
    }

    private class MockHistoryRepository : HistoryRepository {
        val records = mutableListOf<ConversionRecord>()
        override fun getRecentConversions(): Flow<List<ConversionRecord>> = flowOf(records)
        override suspend fun addRecord(record: ConversionRecord): AppResult<Unit> {
            records.add(record)
            return AppResult.Success(Unit)
        }
        override suspend fun deleteRecord(id: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun clearAll(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class MockSettingsRepository : SettingsRepository {
        override fun getSettings(): Flow<UserSettings> = flowOf(UserSettings())
        override fun getSettingsSync(): UserSettings = UserSettings()
        override suspend fun updateTheme(themeMode: com.example.documate.core.domain.model.AppThemeMode): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateDefaultPageSize(pageSize: com.example.documate.core.domain.model.PdfPageSize): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateDefaultCompression(preset: com.example.documate.core.domain.model.CompressionPreset): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateBackendUrl(url: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getAppCacheSizeBytes(): Long = 0L
        override suspend fun clearAppCache(): AppResult<Long> = AppResult.Success(0L)
    }
}
