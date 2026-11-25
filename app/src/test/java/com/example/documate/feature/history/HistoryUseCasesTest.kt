package com.example.documate.feature.history

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.feature.history.domain.usecase.ClearHistoryUseCase
import com.example.documate.feature.history.domain.usecase.DeleteHistoryRecordUseCase
import com.example.documate.feature.history.domain.usecase.GetConversionHistoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HistoryUseCasesTest {

    private lateinit var fakeHistoryRepository: FakeHistoryRepository
    private lateinit var getHistoryUseCase: GetConversionHistoryUseCase
    private lateinit var deleteRecordUseCase: DeleteHistoryRecordUseCase
    private lateinit var clearHistoryUseCase: ClearHistoryUseCase

    private val samplePdfRecord = ConversionRecord(
        id = "rec_pdf_1",
        tool = DocumentTool.IMAGES_TO_PDF,
        sourceFileName = "photo.jpg",
        outputFileName = "photo.pdf",
        outputPath = "/data/photo.pdf",
        originalSizeBytes = 500000L,
        outputSizeBytes = 450000L,
        timestampMillis = 1000L
    )

    private val sampleOfficeRecord = ConversionRecord(
        id = "rec_office_1",
        tool = DocumentTool.WORD_TO_PDF,
        sourceFileName = "contract.docx",
        outputFileName = "contract.pdf",
        outputPath = "/data/contract.pdf",
        originalSizeBytes = 120000L,
        outputSizeBytes = 98000L,
        timestampMillis = 2000L
    )

    @Before
    fun setup() {
        fakeHistoryRepository = FakeHistoryRepository()
        getHistoryUseCase = GetConversionHistoryUseCase(fakeHistoryRepository)
        deleteRecordUseCase = DeleteHistoryRecordUseCase(fakeHistoryRepository)
        clearHistoryUseCase = ClearHistoryUseCase(fakeHistoryRepository)
    }

    @Test
    fun `getHistory without filter returns all records`() = runBlocking {
        fakeHistoryRepository.addRecord(samplePdfRecord)
        fakeHistoryRepository.addRecord(sampleOfficeRecord)

        val result = getHistoryUseCase().first()
        assertEquals(2, result.size)
    }

    @Test
    fun `getHistory with PDF filter returns only PDF category records`() = runBlocking {
        fakeHistoryRepository.addRecord(samplePdfRecord)
        fakeHistoryRepository.addRecord(sampleOfficeRecord)

        val result = getHistoryUseCase(ToolCategory.PDF).first()
        assertEquals(1, result.size)
        assertEquals(DocumentTool.IMAGES_TO_PDF, result[0].tool)
    }

    @Test
    fun `getHistory with OFFICE filter returns only Office category records`() = runBlocking {
        fakeHistoryRepository.addRecord(samplePdfRecord)
        fakeHistoryRepository.addRecord(sampleOfficeRecord)

        val result = getHistoryUseCase(ToolCategory.OFFICE).first()
        assertEquals(1, result.size)
        assertEquals(DocumentTool.WORD_TO_PDF, result[0].tool)
    }

    @Test
    fun `deleteRecord with valid id removes record`() = runBlocking {
        fakeHistoryRepository.addRecord(samplePdfRecord)
        fakeHistoryRepository.addRecord(sampleOfficeRecord)

        val deleteResult = deleteRecordUseCase("rec_pdf_1")
        assertTrue(deleteResult is AppResult.Success)

        val remaining = getHistoryUseCase().first()
        assertEquals(1, remaining.size)
        assertEquals("rec_office_1", remaining[0].id)
    }

    @Test
    fun `deleteRecord with empty id returns error without modifying list`() = runBlocking {
        fakeHistoryRepository.addRecord(samplePdfRecord)

        val deleteResult = deleteRecordUseCase("   ")
        assertTrue(deleteResult is AppResult.Error)

        val remaining = getHistoryUseCase().first()
        assertEquals(1, remaining.size)
    }

    @Test
    fun `clearHistory removes all records`() = runBlocking {
        fakeHistoryRepository.addRecord(samplePdfRecord)
        fakeHistoryRepository.addRecord(sampleOfficeRecord)

        val clearResult = clearHistoryUseCase()
        assertTrue(clearResult is AppResult.Success)

        val remaining = getHistoryUseCase().first()
        assertTrue(remaining.isEmpty())
    }

    private class FakeHistoryRepository : HistoryRepository {
        private val _flow = MutableStateFlow<List<ConversionRecord>>(emptyList())
        var shouldFail = false

        override fun getRecentConversions(): Flow<List<ConversionRecord>> = _flow.asStateFlow()

        override suspend fun addRecord(record: ConversionRecord): AppResult<Unit> {
            if (shouldFail) return AppResult.Error("Simulated add error")
            _flow.value = listOf(record) + _flow.value
            return AppResult.Success(Unit)
        }

        override suspend fun deleteRecord(id: String): AppResult<Unit> {
            if (shouldFail) return AppResult.Error("Simulated delete error")
            _flow.value = _flow.value.filter { it.id != id }
            return AppResult.Success(Unit)
        }

        override suspend fun clearAll(): AppResult<Unit> {
            if (shouldFail) return AppResult.Error("Simulated clear error")
            _flow.value = emptyList()
            return AppResult.Success(Unit)
        }
    }
}
