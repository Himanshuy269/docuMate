package com.example.documate.feature.pdf_compressor

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.pdf_compressor.domain.usecase.CompressPdfUseCase
import com.example.documate.feature.pdf_compressor.presentation.PdfCompressorUiState
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

class PdfCompressorEdgeCasesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: MockDocumentRepository
    private lateinit var fakePdfRepo: MockPdfRepository
    private lateinit var fakeHistoryRepo: MockHistoryRepository
    private lateinit var useCase: CompressPdfUseCase

    private val samplePdf = DocumentFile(
        id = "pdf_big",
        name = "heavy_scan.pdf",
        uriString = "content://docs/heavy",
        sizeBytes = 10000000L,
        mimeType = "application/pdf",
        extension = "pdf"
    )

    @Before
    fun setup() {
        fakeDocRepo = MockDocumentRepository(tempFolder)
        fakePdfRepo = MockPdfRepository(tempFolder)
        fakeHistoryRepo = MockHistoryRepository()
        useCase = CompressPdfUseCase(fakeDocRepo, fakePdfRepo, fakeHistoryRepo)
    }

    @Test
    fun `null source PDF returns validation error`() = runBlocking {
        val result = useCase(
            sourcePdf = null,
            preset = CompressionPreset.MEDIUM,
            outputFileName = "compressed",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Please select a PDF to compress", (result as AppResult.Error).message)
    }

    @Test
    fun `blank output filename generates compressed suffix default`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            preset = CompressionPreset.HIGH,
            outputFileName = "    ",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals("heavy_scan_compressed.pdf", data.name)
    }

    @Test
    fun `compression preset HIGH reduces size and records history`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            preset = CompressionPreset.HIGH,
            outputFileName = "compact.pdf",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals(CompressionPreset.HIGH, fakePdfRepo.lastPreset)
        assertEquals(1, fakeHistoryRepo.records.size)
        val historyRecord = fakeHistoryRepo.records.first()
        assertEquals("heavy_scan.pdf", historyRecord.sourceFileName)
        assertEquals("compact.pdf", historyRecord.outputFileName)
    }

    @Test
    fun `cache error aborts compression pipeline safely`() = runBlocking {
        fakeDocRepo.shouldFailCopy = true
        val result = useCase(
            sourcePdf = samplePdf,
            preset = CompressionPreset.LOW,
            outputFileName = "out",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Failed to access source PDF"))
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    @Test
    fun `engine compression failure does not save history record`() = runBlocking {
        fakePdfRepo.shouldFail = true
        val result = useCase(
            sourcePdf = samplePdf,
            preset = CompressionPreset.MEDIUM,
            outputFileName = "failed",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Compression engine out of memory", (result as AppResult.Error).message)
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    @Test
    fun `uiState canCompress checks requirements`() {
        assertFalse(PdfCompressorUiState(selectedPdf = null).canCompress)
        assertFalse(PdfCompressorUiState(selectedPdf = samplePdf, outputFileName = "").canCompress)
        assertFalse(PdfCompressorUiState(selectedPdf = samplePdf, outputFileName = "   ").canCompress)
        assertFalse(PdfCompressorUiState(selectedPdf = samplePdf, outputFileName = "valid", isCompressing = true).canCompress)
        assertTrue(PdfCompressorUiState(selectedPdf = samplePdf, outputFileName = "valid", isCompressing = false).canCompress)
    }

    // --- Mocks ---

    private class MockDocumentRepository(private val tempFolder: TemporaryFolder) : DocumentRepository {
        var shouldFailCopy = false

        override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> {
            return AppResult.Success(DocumentFile("id", "doc.pdf", uriString, 1000))
        }

        override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File> {
            if (shouldFailCopy) return AppResult.Error("Read permission denied")
            val file = tempFolder.newFile("temp_${System.currentTimeMillis()}_$preferredName")
            file.writeBytes("%PDF-1.4\nHeavyPDF\n%%EOF".toByteArray())
            return AppResult.Success(file)
        }

        override suspend fun exportFile(sourceFile: File, destinationFileName: String): AppResult<String> = AppResult.Success("")
        override suspend fun copyFileToUri(sourceFile: File, destinationUriString: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun getShareableUri(file: File): AppResult<String> = AppResult.Success("content://share/${file.name}")
        override suspend fun clearTemporaryFiles(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class MockPdfRepository(private val tempFolder: TemporaryFolder) : PdfRepository {
        var shouldFail = false
        var lastPreset: CompressionPreset? = null

        override suspend fun compressPdf(
            sourcePdfFile: File,
            preset: CompressionPreset,
            outputFileName: String,
            onProgress: (Float) -> Unit
        ): AppResult<File> {
            if (shouldFail) return AppResult.Error("Compression engine out of memory")
            lastPreset = preset
            val cleanName = if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf"
            val file = tempFolder.newFile(cleanName)
            file.writeBytes("%PDF-1.4\nCompressedContent\n%%EOF".toByteArray())
            onProgress(1.0f)
            return AppResult.Success(file)
        }

        override suspend fun createPdfFromImages(
            imageFiles: List<File>,
            pageSize: PdfPageSize,
            orientation: PdfOrientation,
            outputFileName: String,
            margin: PdfMargin,
            onProgress: (Float) -> Unit
        ): AppResult<File> = AppResult.Error("Not implemented")

        override suspend fun addImagesToPdf(
            existingPdfFile: File,
            imageFiles: List<File>,
            insertPositionPage: Int,
            outputFileName: String,
            onProgress: (Float) -> Unit
        ): AppResult<File> = AppResult.Error("Not implemented")
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
}
