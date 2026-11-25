package com.example.documate.feature.images_to_pdf

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.feature.images_to_pdf.domain.usecase.ConvertImagesToPdfUseCase
import com.example.documate.feature.images_to_pdf.presentation.ImagesToPdfUiEvent
import com.example.documate.feature.images_to_pdf.presentation.ImagesToPdfUiState
import com.example.documate.feature.images_to_pdf.presentation.ImagesToPdfViewModel
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

class ImagesToPdfEdgeCasesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDocRepo: MockDocumentRepository
    private lateinit var fakePdfRepo: MockPdfRepository
    private lateinit var fakeHistoryRepo: MockHistoryRepository
    private lateinit var useCase: ConvertImagesToPdfUseCase

    @Before
    fun setup() {
        fakeDocRepo = MockDocumentRepository(tempFolder)
        fakePdfRepo = MockPdfRepository(tempFolder)
        fakeHistoryRepo = MockHistoryRepository()
        useCase = ConvertImagesToPdfUseCase(fakeDocRepo, fakePdfRepo, fakeHistoryRepo)
    }

    @Test
    fun `blank or whitespace output filename generates default name`() = runBlocking {
        val testImages = listOf(
            DocumentFile(id = "1", name = "img1.png", uriString = "content://1", sizeBytes = 100L)
        )

        val result = useCase(
            selectedImages = testImages,
            pageSize = PdfPageSize.A4,
            orientation = PdfOrientation.PORTRAIT,
            margin = PdfMargin.NONE,
            outputFileName = "   ",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertTrue(data.name.startsWith("Images_") && data.name.endsWith(".pdf"))
    }

    @Test
    fun `output filename already ending with pdf does not duplicate extension`() = runBlocking {
        val testImages = listOf(
            DocumentFile(id = "1", name = "img1.png", uriString = "content://1", sizeBytes = 100L)
        )

        val result = useCase(
            selectedImages = testImages,
            pageSize = PdfPageSize.A4,
            orientation = PdfOrientation.PORTRAIT,
            margin = PdfMargin.NORMAL,
            outputFileName = "MyDocument.pdf",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Success)
        assertEquals("MyDocument.pdf", (result as AppResult.Success).data.name)
    }

    @Test
    fun `cache failure on one image halts pipeline and reports error`() = runBlocking {
        fakeDocRepo.shouldFailCopy = true
        val testImages = listOf(
            DocumentFile(id = "1", name = "broken.jpg", uriString = "content://broken", sizeBytes = 100L)
        )

        val result = useCase(
            selectedImages = testImages,
            pageSize = PdfPageSize.A4,
            orientation = PdfOrientation.PORTRAIT,
            margin = PdfMargin.SMALL,
            outputFileName = "HaltTest",
            onProgress = { _, _ -> }
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Failed to access image"))
        assertTrue(fakeHistoryRepo.records.isEmpty())
    }

    @Test
    fun `uiState canConvert handles edge case combinations`() {
        val emptyState = ImagesToPdfUiState(selectedImages = emptyList(), outputFileName = "Test")
        assertFalse(emptyState.canConvert)

        val blankNameState = ImagesToPdfUiState(
            selectedImages = listOf(DocumentFile(id = "1", name = "1.jpg", uriString = "uri", sizeBytes = 10)),
            outputFileName = "   "
        )
        assertFalse(blankNameState.canConvert)

        val processingState = ImagesToPdfUiState(
            selectedImages = listOf(DocumentFile(id = "1", name = "1.jpg", uriString = "uri", sizeBytes = 10)),
            outputFileName = "ValidName",
            isProcessing = true
        )
        assertFalse(processingState.canConvert)

        val validState = ImagesToPdfUiState(
            selectedImages = listOf(DocumentFile(id = "1", name = "1.jpg", uriString = "uri", sizeBytes = 10)),
            outputFileName = "ValidName",
            isProcessing = false
        )
        assertTrue(validState.canConvert)
    }

    @Test
    fun `move image out of bounds preserves list integrity`() {
        val file1 = DocumentFile(id = "1", name = "1.jpg", uriString = "u1", sizeBytes = 10)
        val file2 = DocumentFile(id = "2", name = "2.jpg", uriString = "u2", sizeBytes = 10)
        val list = listOf(file1, file2)

        fun moveItem(items: List<DocumentFile>, fromIndex: Int, toIndex: Int): List<DocumentFile> {
            return if (fromIndex in items.indices && toIndex in items.indices) {
                val updated = items.toMutableList()
                val item = updated.removeAt(fromIndex)
                updated.add(toIndex, item)
                updated
            } else items
        }

        assertEquals(list, moveItem(list, -1, 0))
        assertEquals(list, moveItem(list, 0, 99))
        assertEquals(list, moveItem(list, 5, -2))
        assertEquals(listOf(file2, file1), moveItem(list, 0, 1))
    }

    @Test
    fun `remove image out of bounds preserves list integrity`() {
        val file1 = DocumentFile(id = "1", name = "1.jpg", uriString = "u1", sizeBytes = 10)
        val list = listOf(file1)

        fun removeItem(items: List<DocumentFile>, index: Int): List<DocumentFile> {
            return if (index in items.indices) {
                items.toMutableList().apply { removeAt(index) }
            } else items
        }

        assertEquals(list, removeItem(list, -1))
        assertEquals(list, removeItem(list, 100))
        assertEquals(emptyList<DocumentFile>(), removeItem(list, 0))
    }

    // --- Mock Repositories ---

    private class MockDocumentRepository(private val tempFolder: TemporaryFolder) : DocumentRepository {
        var shouldFailCopy = false

        override suspend fun resolveFileMetadata(uriString: String): AppResult<DocumentFile> {
            return AppResult.Success(
                DocumentFile(id = "id", name = "name.jpg", uriString = uriString, sizeBytes = 500)
            )
        }

        override suspend fun copyToAppCache(uriString: String, preferredName: String): AppResult<File> {
            if (shouldFailCopy) return AppResult.Error("Disk full or unreadable stream")
            val file = tempFolder.newFile("cache_${System.currentTimeMillis()}_$preferredName")
            file.writeBytes("dummy-data".toByteArray())
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

        override fun getShareableUri(file: File): AppResult<String> = AppResult.Success("content://share/${file.name}")

        override suspend fun clearTemporaryFiles(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class MockPdfRepository(private val tempFolder: TemporaryFolder) : PdfRepository {
        override suspend fun createPdfFromImages(
            imageFiles: List<File>,
            pageSize: PdfPageSize,
            orientation: PdfOrientation,
            outputFileName: String,
            margin: PdfMargin,
            onProgress: (Float) -> Unit
        ): AppResult<File> {
            val cleanName = if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf"
            val file = tempFolder.newFile(cleanName)
            file.writeBytes("%PDF-1.4\nEdgeCaseTest\n%%EOF".toByteArray())
            onProgress(1.0f)
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

    private class MockHistoryRepository : HistoryRepository {
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
