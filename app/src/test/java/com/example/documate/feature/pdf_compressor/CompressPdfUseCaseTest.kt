package com.example.documate.feature.pdf_compressor

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.pdf_compressor.domain.usecase.CompressPdfUseCase
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

class CompressPdfUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: FakeDocumentRepository
    private lateinit var fakePdfRepo: FakePdfRepository
    private lateinit var fakeHistoryRepo: FakeHistoryRepository
    private lateinit var useCase: CompressPdfUseCase

    private val samplePdf = DocumentFile(
        id = "pdf_doc_1",
        name = "large_presentation.pdf",
        uriString = "content://docs/large_presentation.pdf",
        sizeBytes = 15728640L, // 15 MB
        mimeType = "application/pdf",
        extension = "pdf"
    )

    @Before
    fun setup() {
        fakeDocRepo = FakeDocumentRepository(tempFolder)
        fakePdfRepo = FakePdfRepository(tempFolder)
        fakeHistoryRepo = FakeHistoryRepository()
        useCase = CompressPdfUseCase(
            documentRepository = fakeDocRepo,
            pdfRepository = fakePdfRepo,
            historyRepository = fakeHistoryRepo
        )
    }

    @Test
    fun `null sourcePdf returns validation error`() = runBlocking {
        val result = useCase(
            sourcePdf = null,
            preset = CompressionPreset.MEDIUM,
            outputFileName = "Compressed",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Please select a PDF to compress", (result as AppResult.Error).message)
    }

    @Test
    fun `valid source pdf compresses successfully and records history`() = runBlocking {
        var lastProgress = 0f
        var lastStage = ""

        val result = useCase(
            sourcePdf = samplePdf,
            preset = CompressionPreset.MEDIUM,
            outputFileName = "Slim_Presentation",
            onProgress = { p, stage ->
                lastProgress = p
                lastStage = stage
            }
        )

        assertTrue("Expected Success, got $result", result is AppResult.Success)
        val doc = (result as AppResult.Success).data
        assertEquals("Slim_Presentation.pdf", doc.name)
        assertEquals("application/pdf", doc.mimeType)
        assertEquals(1.0f, lastProgress, 0.01f)
        assertEquals("Complete!", lastStage)

        // Verify history was recorded
        assertEquals(1, fakeHistoryRepo.records.size)
        val record = fakeHistoryRepo.records.first()
        assertEquals(DocumentTool.PDF_COMPRESSION, record.tool)
        assertEquals("large_presentation.pdf", record.sourceFileName)
        assertEquals("Slim_Presentation.pdf", record.outputFileName)
        assertEquals(15728640L, record.originalSizeBytes)
    }

    @Test
    fun `high compression preset passes correct preset to repository`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            preset = CompressionPreset.HIGH,
            outputFileName = "HighComp",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals(CompressionPreset.HIGH, fakePdfRepo.lastPreset)
    }

    @Test
    fun `low compression preset passes correct preset to repository`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            preset = CompressionPreset.LOW,
            outputFileName = "LowComp",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals(CompressionPreset.LOW, fakePdfRepo.lastPreset)
    }

    @Test
    fun `compression engine failure returns error and does not record history`() = runBlocking {
        fakePdfRepo.shouldFail = true

        val result = useCase(
            sourcePdf = samplePdf,
            preset = CompressionPreset.MEDIUM,
            outputFileName = "FailRun",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Simulated PDF compression engine error", (result as AppResult.Error).message)
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    @Test
    fun `source pdf copy failure returns error`() = runBlocking {
        fakeDocRepo.shouldFailCopy = true

        val result = useCase(
            sourcePdf = samplePdf,
            preset = CompressionPreset.MEDIUM,
            outputFileName = "FailRun",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Failed to access source PDF"))
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    // --- Fakes for CompressPdfUseCase tests ---

    private class FakeDocumentRepository(private val tempFolder: TemporaryFolder) : DocumentRepository {
        var shouldFailCopy = false

        override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> {
            return AppResult.Success(
                DocumentFile(
                    id = "fake",
                    name = "fake.pdf",
                    uriString = uriString,
                    sizeBytes = 1000L,
                    mimeType = "application/pdf",
                    extension = "pdf"
                )
            )
        }

        override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File> {
            if (shouldFailCopy) return AppResult.Error("Disk full or permission denied")
            val file = tempFolder.newFile("temp_${System.currentTimeMillis()}_$preferredName")
            file.writeBytes("dummy-pdf-content".toByteArray())
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

    private class FakePdfRepository(private val tempFolder: TemporaryFolder) : PdfRepository {
        var shouldFail = false
        var lastPreset: CompressionPreset? = null

        override suspend fun createPdfFromImages(
            imageFiles: List<File>,
            pageSize: PdfPageSize,
            orientation: PdfOrientation,
            outputFileName: String,
            margin: PdfMargin,
            onProgress: (Float) -> Unit
        ): AppResult<File> = AppResult.Error("Not needed")

        override suspend fun addImagesToPdf(
            existingPdfFile: File,
            imageFiles: List<File>,
            insertPositionPage: Int,
            outputFileName: String,
            onProgress: (Float) -> Unit
        ): AppResult<File> = AppResult.Error("Not needed")

        override suspend fun compressPdf(
            sourcePdfFile: File,
            preset: CompressionPreset,
            outputFileName: String,
            onProgress: (Float) -> Unit
        ): AppResult<File> {
            if (shouldFail) return AppResult.Error("Simulated PDF compression engine error")
            lastPreset = preset
            onProgress(1.0f)
            val cleanName = if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf"
            val file = tempFolder.newFile(cleanName)
            file.writeBytes("%PDF-1.4\nCompressedContent\n%%EOF".toByteArray())
            return AppResult.Success(file)
        }
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
