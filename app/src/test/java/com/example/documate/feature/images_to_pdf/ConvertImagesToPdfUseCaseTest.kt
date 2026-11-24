package com.example.documate.feature.images_to_pdf

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.images_to_pdf.domain.usecase.ConvertImagesToPdfUseCase
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

class ConvertImagesToPdfUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: FakeDocumentRepository
    private lateinit var fakePdfRepo: FakePdfRepository
    private lateinit var fakeHistoryRepo: FakeHistoryRepository
    private lateinit var useCase: ConvertImagesToPdfUseCase

    @Before
    fun setup() {
        fakeDocRepo = FakeDocumentRepository(tempFolder)
        fakePdfRepo = FakePdfRepository(tempFolder)
        fakeHistoryRepo = FakeHistoryRepository()
        useCase = ConvertImagesToPdfUseCase(
            documentRepository = fakeDocRepo,
            pdfRepository = fakePdfRepo,
            historyRepository = fakeHistoryRepo
        )
    }

    @Test
    fun `empty images list returns error`() = runBlocking {
        val result = useCase(
            selectedImages = emptyList(),
            pageSize = PdfPageSize.A4,
            orientation = PdfOrientation.PORTRAIT,
            margin = PdfMargin.NONE,
            outputFileName = "TestOutput",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("No images selected for conversion", (result as AppResult.Error).message)
    }

    @Test
    fun `valid images convert successfully and record history`() = runBlocking {
        val testImages = listOf(
            DocumentFile(
                id = "1",
                name = "photo1.jpg",
                uriString = "content://media/1",
                sizeBytes = 2048L,
                mimeType = "image/jpeg",
                extension = "jpg"
            ),
            DocumentFile(
                id = "2",
                name = "photo2.png",
                uriString = "content://media/2",
                sizeBytes = 4096L,
                mimeType = "image/png",
                extension = "png"
            )
        )

        var lastProgress = 0f
        var lastStage = ""

        val result = useCase(
            selectedImages = testImages,
            pageSize = PdfPageSize.A4,
            orientation = PdfOrientation.PORTRAIT,
            margin = PdfMargin.SMALL,
            outputFileName = "MyAlbum",
            onProgress = { p, stage ->
                lastProgress = p
                lastStage = stage
            }
        )

        assertTrue("Expected AppResult.Success but got $result", result is AppResult.Success)
        val doc = (result as AppResult.Success).data
        assertEquals("MyAlbum.pdf", doc.name)
        assertEquals("application/pdf", doc.mimeType)
        assertEquals(1.0f, lastProgress, 0.01f)
        assertEquals("Complete!", lastStage)

        // Verify history was recorded
        assertEquals(1, fakeHistoryRepo.records.size)
        val historyItem = fakeHistoryRepo.records.first()
        assertEquals(DocumentTool.IMAGES_TO_PDF, historyItem.tool)
        assertEquals("2 images", historyItem.sourceFileName)
        assertEquals("MyAlbum.pdf", historyItem.outputFileName)
        assertEquals(6144L, historyItem.originalSizeBytes)
    }

    @Test
    fun `pdf generation failure returns error and does not record history`() = runBlocking {
        fakePdfRepo.shouldFail = true
        val testImages = listOf(
            DocumentFile(
                id = "1",
                name = "photo.jpg",
                uriString = "content://media/1",
                sizeBytes = 1024L,
                mimeType = "image/jpeg",
                extension = "jpg"
            )
        )

        val result = useCase(
            selectedImages = testImages,
            pageSize = PdfPageSize.LETTER,
            orientation = PdfOrientation.LANDSCAPE,
            margin = PdfMargin.NORMAL,
            outputFileName = "FailedRun",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertEquals("Simulated PDF engine error", (result as AppResult.Error).message)
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    // --- Fakes for Unit Testing ---

    private class FakeDocumentRepository(private val tempFolder: TemporaryFolder) : DocumentRepository {
        var shouldFailCopy = false

        override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> {
            return AppResult.Success(
                DocumentFile(
                    id = "fake",
                    name = "fake.jpg",
                    uriString = uriString,
                    sizeBytes = 1000L,
                    mimeType = "image/jpeg",
                    extension = "jpg"
                )
            )
        }

        override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File> {
            if (shouldFailCopy) return AppResult.Error("Disk error")
            val file = tempFolder.newFile("temp_$preferredName")
            file.writeBytes("dummy-image-bytes".toByteArray())
            return AppResult.Success(file)
        }

        override suspend fun exportFile(sourceFile: File, destinationFileName: String): AppResult<String> {
            val dest = tempFolder.newFile(destinationFileName)
            sourceFile.copyTo(dest, overwrite = true)
            return AppResult.Success(dest.absolutePath)
        }

        override suspend fun copyFileToUri(sourceFile: File, destinationUriString: String): AppResult<Unit> {
            return AppResult.Success(Unit)
        }

        override fun getShareableUri(file: File): AppResult<String> {
            return AppResult.Success("content://com.example.documate.fileprovider/fake/${file.name}")
        }

        override suspend fun clearTemporaryFiles(): AppResult<Unit> {
            return AppResult.Success(Unit)
        }
    }

    private class FakePdfRepository(private val tempFolder: TemporaryFolder) : PdfRepository {
        var shouldFail = false

        override suspend fun createPdfFromImages(
            imageFiles: List<File>,
            pageSize: PdfPageSize,
            orientation: PdfOrientation,
            outputFileName: String,
            margin: PdfMargin,
            onProgress: (Float) -> Unit
        ): AppResult<File> {
            if (shouldFail) return AppResult.Error("Simulated PDF engine error")
            onProgress(1.0f)
            val cleanName = if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf"
            val file = tempFolder.newFile(cleanName)
            file.writeBytes("%PDF-1.4\nTest\n%%EOF".toByteArray())
            return AppResult.Success(file)
        }

        override suspend fun addImagesToPdf(
            existingPdfFile: File,
            imageFiles: List<File>,
            insertPositionPage: Int,
            outputFileName: String,
            onProgress: (Float) -> Unit
        ): AppResult<File> = AppResult.Error("Not implemented")

        override suspend fun compressPdf(
            sourcePdfFile: File,
            preset: CompressionPreset,
            outputFileName: String,
            onProgress: (Float) -> Unit
        ): AppResult<File> = AppResult.Error("Not implemented")
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
