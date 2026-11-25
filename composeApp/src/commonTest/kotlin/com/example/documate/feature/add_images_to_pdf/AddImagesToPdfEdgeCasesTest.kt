package com.example.documate.feature.add_images_to_pdf

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
import com.example.documate.feature.add_images_to_pdf.domain.usecase.AddImagesToPdfUseCase
import com.example.documate.feature.add_images_to_pdf.presentation.AddImagesToPdfUiState
import com.example.documate.feature.add_images_to_pdf.presentation.InsertPosition
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

class AddImagesToPdfEdgeCasesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: MockDocumentRepository
    private lateinit var fakePdfRepo: MockPdfRepository
    private lateinit var fakeHistoryRepo: MockHistoryRepository
    private lateinit var useCase: AddImagesToPdfUseCase

    private val samplePdf = DocumentFile(
        id = "pdf_1",
        name = "annual_report.pdf",
        uriString = "content://docs/1",
        sizeBytes = 50000L,
        mimeType = "application/pdf",
        extension = "pdf"
    )

    private val sampleImage = DocumentFile(
        id = "img_1",
        name = "appendix.png",
        uriString = "content://images/1",
        sizeBytes = 12000L,
        mimeType = "image/png",
        extension = "png"
    )

    @Before
    fun setup() {
        fakeDocRepo = MockDocumentRepository(tempFolder)
        fakePdfRepo = MockPdfRepository(tempFolder)
        fakeHistoryRepo = MockHistoryRepository()
        useCase = AddImagesToPdfUseCase(fakeDocRepo, fakePdfRepo, fakeHistoryRepo)
    }

    @Test
    fun `null source PDF returns validation error`() = runBlocking {
        val result = useCase(
            sourcePdf = null,
            selectedImages = listOf(sampleImage),
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "out",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Please select a source PDF document", (result as AppResult.Error).message)
    }

    @Test
    fun `empty images list returns validation error`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = emptyList(),
            insertPosition = InsertPosition.AT_BEGINNING,
            specificPageNumber = 1,
            outputFileName = "out",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Please select at least one image to insert", (result as AppResult.Error).message)
    }

    @Test
    fun `insert at beginning passes page index 0 to repository`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = listOf(sampleImage),
            insertPosition = InsertPosition.AT_BEGINNING,
            specificPageNumber = 1,
            outputFileName = "InsertedBeginning",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals(0, fakePdfRepo.lastInsertPositionPage)
    }

    @Test
    fun `insert at end passes page index -1 to repository`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = listOf(sampleImage),
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "InsertedEnd",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals(-1, fakePdfRepo.lastInsertPositionPage)
    }

    @Test
    fun `insert at specific page coerces negative page number to at least 1`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = listOf(sampleImage),
            insertPosition = InsertPosition.SPECIFIC_PAGE,
            specificPageNumber = -5,
            outputFileName = "InsertedSpecific",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, fakePdfRepo.lastInsertPositionPage)
    }

    @Test
    fun `blank output filename generates modified suffix default`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = listOf(sampleImage),
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "    ",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals("annual_report_modified.pdf", data.name)
    }

    @Test
    fun `source pdf caching error aborts pipeline`() = runBlocking {
        fakeDocRepo.shouldFailPdfCopy = true
        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = listOf(sampleImage),
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "out",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Failed to access source PDF"))
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    @Test
    fun `uiState canProcess checks all requirements`() {
        assertFalse(AddImagesToPdfUiState(selectedPdf = null).canProcess)
        assertFalse(AddImagesToPdfUiState(selectedPdf = samplePdf, selectedImages = emptyList()).canProcess)
        assertFalse(AddImagesToPdfUiState(selectedPdf = samplePdf, selectedImages = listOf(sampleImage), outputFileName = "").canProcess)
        assertFalse(AddImagesToPdfUiState(selectedPdf = samplePdf, selectedImages = listOf(sampleImage), outputFileName = "out", isProcessing = true).canProcess)
        assertTrue(AddImagesToPdfUiState(selectedPdf = samplePdf, selectedImages = listOf(sampleImage), outputFileName = "out", isProcessing = false).canProcess)
    }

    // --- Mocks ---

    private class MockDocumentRepository(private val tempFolder: TemporaryFolder) : DocumentRepository {
        var shouldFailPdfCopy = false

        override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> {
            return AppResult.Success(DocumentFile("id", "doc.pdf", uriString, 1000))
        }

        override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File> {
            if (shouldFailPdfCopy && preferredName.endsWith(".pdf")) {
                return AppResult.Error("Access denied")
            }
            val file = tempFolder.newFile("temp_${System.currentTimeMillis()}_$preferredName")
            file.writeBytes("%PDF-1.4\nSample\n%%EOF".toByteArray())
            return AppResult.Success(file)
        }

        override suspend fun exportFile(sourceFile: File, destinationFileName: String): AppResult<String> = AppResult.Success("")
        override suspend fun copyFileToUri(sourceFile: File, destinationUriString: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun getShareableUri(file: File): AppResult<String> = AppResult.Success("content://share/${file.name}")
        override suspend fun clearTemporaryFiles(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class MockPdfRepository(private val tempFolder: TemporaryFolder) : PdfRepository {
        var lastInsertPositionPage: Int = -99

        override suspend fun addImagesToPdf(
            existingPdfFile: File,
            imageFiles: List<File>,
            insertPositionPage: Int,
            outputFileName: String,
            onProgress: (Float) -> Unit
        ): AppResult<File> {
            lastInsertPositionPage = insertPositionPage
            val cleanName = if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf"
            val file = tempFolder.newFile(cleanName)
            file.writeBytes("%PDF-1.4\nMerged\n%%EOF".toByteArray())
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

        override suspend fun compressPdf(
            sourcePdfFile: File,
            preset: CompressionPreset,
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
