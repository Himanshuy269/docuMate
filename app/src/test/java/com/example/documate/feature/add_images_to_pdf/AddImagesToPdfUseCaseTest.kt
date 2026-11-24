package com.example.documate.feature.add_images_to_pdf

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
import com.example.documate.feature.add_images_to_pdf.domain.usecase.AddImagesToPdfUseCase
import com.example.documate.feature.add_images_to_pdf.presentation.InsertPosition
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

class AddImagesToPdfUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: FakeDocumentRepository
    private lateinit var fakePdfRepo: FakePdfRepository
    private lateinit var fakeHistoryRepo: FakeHistoryRepository
    private lateinit var useCase: AddImagesToPdfUseCase

    private val samplePdf = DocumentFile(
        id = "pdf_1",
        name = "annual_report.pdf",
        uriString = "content://docs/annual_report.pdf",
        sizeBytes = 1048576L,
        mimeType = "application/pdf",
        extension = "pdf"
    )

    private val sampleImages = listOf(
        DocumentFile(
            id = "img_1",
            name = "chart1.jpg",
            uriString = "content://media/chart1.jpg",
            sizeBytes = 204800L,
            mimeType = "image/jpeg",
            extension = "jpg"
        ),
        DocumentFile(
            id = "img_2",
            name = "signature.png",
            uriString = "content://media/signature.png",
            sizeBytes = 102400L,
            mimeType = "image/png",
            extension = "png"
        )
    )

    @Before
    fun setup() {
        fakeDocRepo = FakeDocumentRepository(tempFolder)
        fakePdfRepo = FakePdfRepository(tempFolder)
        fakeHistoryRepo = FakeHistoryRepository()
        useCase = AddImagesToPdfUseCase(
            documentRepository = fakeDocRepo,
            pdfRepository = fakePdfRepo,
            historyRepository = fakeHistoryRepo
        )
    }

    @Test
    fun `null sourcePdf returns validation error`() = runBlocking {
        val result = useCase(
            sourcePdf = null,
            selectedImages = sampleImages,
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "Merged",
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
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "Merged",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Please select at least one image to insert", (result as AppResult.Error).message)
    }

    @Test
    fun `valid inputs insert at end successfully and record history`() = runBlocking {
        var lastProgress = 0f
        var lastStage = ""

        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = sampleImages,
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "Annual_Report_Extended",
            onProgress = { p, stage ->
                lastProgress = p
                lastStage = stage
            }
        )

        assertTrue("Expected Success, got $result", result is AppResult.Success)
        val doc = (result as AppResult.Success).data
        assertEquals("Annual_Report_Extended.pdf", doc.name)
        assertEquals("application/pdf", doc.mimeType)
        assertEquals(1.0f, lastProgress, 0.01f)
        assertEquals("Complete!", lastStage)

        // Verify history was recorded
        assertEquals(1, fakeHistoryRepo.records.size)
        val historyRecord = fakeHistoryRepo.records.first()
        assertEquals(DocumentTool.ADD_IMAGES_TO_PDF, historyRecord.tool)
        assertEquals("annual_report.pdf + 2 image(s)", historyRecord.sourceFileName)
        assertEquals("Annual_Report_Extended.pdf", historyRecord.outputFileName)
        assertEquals(1048576L + 204800L + 102400L, historyRecord.originalSizeBytes)
    }

    @Test
    fun `valid inputs insert at beginning passes position 0 to repository`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = sampleImages,
            insertPosition = InsertPosition.AT_BEGINNING,
            specificPageNumber = 1,
            outputFileName = "BeginningInsert",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals(0, fakePdfRepo.lastInsertPositionPage)
    }

    @Test
    fun `valid inputs insert at specific page passes correct page to repository`() = runBlocking {
        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = sampleImages,
            insertPosition = InsertPosition.SPECIFIC_PAGE,
            specificPageNumber = 4,
            outputFileName = "Page4Insert",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals(4, fakePdfRepo.lastInsertPositionPage)
    }

    @Test
    fun `pdf repository engine failure returns error and does not record history`() = runBlocking {
        fakePdfRepo.shouldFail = true

        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = sampleImages,
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "FailRun",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Simulated PDF engine error", (result as AppResult.Error).message)
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    @Test
    fun `source pdf copy failure returns error`() = runBlocking {
        fakeDocRepo.failOnPdfCopy = true

        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = sampleImages,
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "FailRun",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Failed to access source PDF"))
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    @Test
    fun `image copy failure returns error`() = runBlocking {
        fakeDocRepo.failOnImageCopy = true

        val result = useCase(
            sourcePdf = samplePdf,
            selectedImages = sampleImages,
            insertPosition = InsertPosition.AT_END,
            specificPageNumber = 1,
            outputFileName = "FailRun",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Failed to access image"))
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    // --- Fakes for AddImagesToPdf tests ---

    private class FakeDocumentRepository(private val tempFolder: TemporaryFolder) : DocumentRepository {
        var failOnPdfCopy = false
        var failOnImageCopy = false

        override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> {
            return AppResult.Success(
                DocumentFile(
                    id = "fake",
                    name = "fake_file",
                    uriString = uriString,
                    sizeBytes = 1000L,
                    mimeType = "application/octet-stream",
                    extension = "dat"
                )
            )
        }

        override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File> {
            if (failOnPdfCopy && preferredName.endsWith(".pdf")) {
                return AppResult.Error("Cannot read source PDF")
            }
            if (failOnImageCopy && !preferredName.endsWith(".pdf")) {
                return AppResult.Error("Cannot read source image")
            }
            val file = tempFolder.newFile("temp_${System.currentTimeMillis()}_$preferredName")
            file.writeBytes("dummy-content".toByteArray())
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
        var lastInsertPositionPage = -1

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
        ): AppResult<File> {
            if (shouldFail) return AppResult.Error("Simulated PDF engine error")
            lastInsertPositionPage = insertPositionPage
            onProgress(1.0f)
            val cleanName = if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf"
            val file = tempFolder.newFile(cleanName)
            file.writeBytes("%PDF-1.4\nModified\n%%EOF".toByteArray())
            return AppResult.Success(file)
        }

        override suspend fun compressPdf(
            sourcePdfFile: File,
            preset: CompressionPreset,
            outputFileName: String,
            onProgress: (Float) -> Unit
        ): AppResult<File> = AppResult.Error("Not needed")
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
