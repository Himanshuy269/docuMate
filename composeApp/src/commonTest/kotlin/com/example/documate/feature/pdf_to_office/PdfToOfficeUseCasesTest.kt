package com.example.documate.feature.pdf_to_office

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PlatformFile
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionOptions
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.feature.pdf_to_excel.domain.usecase.ConvertPdfToExcelUseCase
import com.example.documate.feature.pdf_to_powerpoint.domain.usecase.ConvertPdfToPowerPointUseCase
import com.example.documate.feature.pdf_to_word.domain.usecase.ConvertPdfToWordUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PdfToOfficeUseCasesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: FakeDocumentRepository
    private lateinit var fakeOfficeRepo: FakeOfficeConversionRepository
    private lateinit var fakeHistoryRepo: FakeHistoryRepository

    private lateinit var pdfToWordUseCase: ConvertPdfToWordUseCase
    private lateinit var pdfToExcelUseCase: ConvertPdfToExcelUseCase
    private lateinit var pdfToPowerPointUseCase: ConvertPdfToPowerPointUseCase

    private val samplePdf = DocumentFile(
        id = "pdf_sample_1",
        name = "test_document.pdf",
        uriString = "content://docs/test_document.pdf",
        sizeBytes = 1048576L,
        mimeType = "application/pdf",
        extension = "pdf"
    )

    @Before
    fun setup() {
        fakeDocRepo = FakeDocumentRepository(tempFolder)
        fakeOfficeRepo = FakeOfficeConversionRepository(tempFolder)
        fakeHistoryRepo = FakeHistoryRepository()

        pdfToWordUseCase = ConvertPdfToWordUseCase(fakeDocRepo, fakeOfficeRepo, fakeHistoryRepo)
        pdfToExcelUseCase = ConvertPdfToExcelUseCase(fakeDocRepo, fakeOfficeRepo, fakeHistoryRepo)
        pdfToPowerPointUseCase = ConvertPdfToPowerPointUseCase(fakeDocRepo, fakeOfficeRepo, fakeHistoryRepo)
    }

    // --- PDF TO WORD TESTS ---

    @Test
    fun `pdf to word with null file returns error`() = runBlocking {
        val result = pdfToWordUseCase(
            sourceFile = null,
            hasUserConsented = true,
            backendBaseUrl = "http://10.0.2.2:8080"
        )
        assertTrue(result is AppResult.Error)
        assertEquals("No PDF document selected", (result as AppResult.Error).message)
    }

    @Test
    fun `pdf to word without consent returns error`() = runBlocking {
        val result = pdfToWordUseCase(
            sourceFile = samplePdf,
            hasUserConsented = false,
            backendBaseUrl = "http://10.0.2.2:8080"
        )
        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Explicit consent required"))
    }

    @Test
    fun `pdf to word with non-pdf extension returns error`() = runBlocking {
        val nonPdf = samplePdf.copy(extension = "docx", name = "report.docx")
        val result = pdfToWordUseCase(
            sourceFile = nonPdf,
            hasUserConsented = true,
            backendBaseUrl = "http://10.0.2.2:8080"
        )
        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Only .pdf is supported"))
    }

    @Test
    fun `pdf to word successful conversion creates docx and history record`() = runBlocking {
        var progressReported = false
        val result = pdfToWordUseCase(
            sourceFile = samplePdf,
            hasUserConsented = true,
            backendBaseUrl = "http://10.0.2.2:8080",
            customOutputName = "my_converted_word",
            onProgress = { p, _ -> if (p > 0f) progressReported = true }
        )

        assertTrue(result is AppResult.Success)
        val file = (result as AppResult.Success).data
        assertEquals("my_converted_word.docx", file.name)
        assertEquals("docx", file.extension)
        assertTrue(progressReported)

        // Verify history record
        assertEquals(1, fakeHistoryRepo.records.size)
        val record = fakeHistoryRepo.records.first()
        assertEquals(DocumentTool.PDF_TO_WORD, record.tool)
        assertEquals("my_converted_word.docx", record.outputFileName)
    }

    // --- PDF TO EXCEL TESTS ---

    @Test
    fun `pdf to excel without consent returns error`() = runBlocking {
        val result = pdfToExcelUseCase(
            sourceFile = samplePdf,
            hasUserConsented = false,
            backendBaseUrl = "http://10.0.2.2:8080"
        )
        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Explicit consent required"))
    }

    @Test
    fun `pdf to excel successful conversion creates xlsx`() = runBlocking {
        val result = pdfToExcelUseCase(
            sourceFile = samplePdf,
            hasUserConsented = true,
            backendBaseUrl = "http://10.0.2.2:8080",
            customOutputName = "my_table"
        )

        assertTrue(result is AppResult.Success)
        val file = (result as AppResult.Success).data
        assertEquals("my_table.xlsx", file.name)
        assertEquals("xlsx", file.extension)

        assertEquals(1, fakeHistoryRepo.records.size)
        assertEquals(DocumentTool.PDF_TO_EXCEL, fakeHistoryRepo.records.first().tool)
    }

    // --- PDF TO POWERPOINT TESTS ---

    @Test
    fun `pdf to powerpoint without consent returns error`() = runBlocking {
        val result = pdfToPowerPointUseCase(
            sourceFile = samplePdf,
            hasUserConsented = false,
            backendBaseUrl = "http://10.0.2.2:8080"
        )
        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Explicit consent required"))
    }

    @Test
    fun `pdf to powerpoint successful conversion creates pptx`() = runBlocking {
        val result = pdfToPowerPointUseCase(
            sourceFile = samplePdf,
            hasUserConsented = true,
            backendBaseUrl = "http://10.0.2.2:8080",
            customOutputName = "my_presentation"
        )

        assertTrue(result is AppResult.Success)
        val file = (result as AppResult.Success).data
        assertEquals("my_presentation.pptx", file.name)
        assertEquals("pptx", file.extension)

        assertEquals(1, fakeHistoryRepo.records.size)
        assertEquals(DocumentTool.PDF_TO_POWERPOINT, fakeHistoryRepo.records.first().tool)
    }

    // --- Fakes ---

    private class FakeDocumentRepository(private val tempFolder: TemporaryFolder) : DocumentRepository {
        override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> =
            AppResult.Success(DocumentFile("id", "doc.pdf", uriString, 1000L, "application/pdf", "pdf"))

        override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<PlatformFile> {
            val file = tempFolder.newFile("temp_${System.currentTimeMillis()}_$preferredName")
            file.writeBytes("%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".toByteArray())
            return AppResult.Success(PlatformFile(file.absolutePath, file.name, file.length()))
        }

        override suspend fun exportFile(sourceFile: PlatformFile, destinationFileName: String): AppResult<String> =
            AppResult.Success(sourceFile.path)

        override suspend fun copyFileToUri(sourceFile: PlatformFile, destinationUriString: String): AppResult<Unit> =
            AppResult.Success(Unit)

        override fun getShareableUri(file: PlatformFile): AppResult<String> =
            AppResult.Success("content://share/${file.name}")

        override suspend fun clearTemporaryFiles(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeOfficeConversionRepository(private val tempFolder: TemporaryFolder) : OfficeConversionRepository {
        override suspend fun convertOfficeDocumentToPdf(
            sourceFile: PlatformFile,
            tool: DocumentTool,
            backendBaseUrl: String,
            authToken: String?,
            options: OfficeConversionOptions,
            onProgress: (Float) -> Unit
        ): AppResult<PlatformFile> = AppResult.Success(sourceFile)

        override suspend fun convertPdfToOfficeDocument(
            sourceFile: PlatformFile,
            tool: DocumentTool,
            backendBaseUrl: String,
            authToken: String?,
            outputFileName: String?,
            onProgress: (Float) -> Unit
        ): AppResult<PlatformFile> {
            onProgress(1.0f)
            val ext = when (tool) {
                DocumentTool.PDF_TO_WORD -> "docx"
                DocumentTool.PDF_TO_EXCEL -> "xlsx"
                DocumentTool.PDF_TO_POWERPOINT -> "pptx"
                else -> "docx"
            }
            val baseName = outputFileName?.let { if (it.endsWith(".$ext")) it else "$it.$ext" }
                ?: "${File(sourceFile.path).nameWithoutExtension}_converted.$ext"
            val file = tempFolder.newFile(baseName)
            file.writeBytes("PK-OpenXml-Fake-Content".toByteArray())
            return AppResult.Success(PlatformFile(file.absolutePath, file.name, file.length()))
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
        override suspend fun deleteRecord(id: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun clearAll(): AppResult<Unit> = AppResult.Success(Unit)
    }
}
