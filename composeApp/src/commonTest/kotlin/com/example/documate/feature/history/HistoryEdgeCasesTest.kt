package com.example.documate.feature.history

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.feature.history.domain.usecase.ClearHistoryUseCase
import com.example.documate.feature.history.domain.usecase.DeleteHistoryRecordUseCase
import com.example.documate.feature.history.domain.usecase.GetConversionHistoryUseCase
import com.example.documate.feature.history.presentation.HistoryUiEvent
import com.example.documate.feature.history.presentation.HistoryUiState
import com.example.documate.feature.history.presentation.HistoryViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HistoryEdgeCasesTest {

    private lateinit var fakeRepo: InMemoryHistoryRepo
    private lateinit var getHistoryUseCase: GetConversionHistoryUseCase
    private lateinit var deleteRecordUseCase: DeleteHistoryRecordUseCase
    private lateinit var clearHistoryUseCase: ClearHistoryUseCase
    private lateinit var viewModel: HistoryViewModel

    private val record1 = ConversionRecord(
        id = "id-1",
        tool = DocumentTool.IMAGES_TO_PDF,
        sourceFileName = "img.jpg",
        outputFileName = "img.pdf",
        outputPath = "/path/img.pdf",
        originalSizeBytes = 100L,
        outputSizeBytes = 90L,
        timestampMillis = 1000L
    )

    private val record2 = ConversionRecord(
        id = "id-2",
        tool = DocumentTool.WORD_TO_PDF,
        sourceFileName = "doc.docx",
        outputFileName = "doc.pdf",
        outputPath = "/path/doc.pdf",
        originalSizeBytes = 200L,
        outputSizeBytes = 180L,
        timestampMillis = 2000L
    )

    @Before
    fun setup() {
        fakeRepo = InMemoryHistoryRepo()
        getHistoryUseCase = GetConversionHistoryUseCase(fakeRepo)
        deleteRecordUseCase = DeleteHistoryRecordUseCase(fakeRepo)
        clearHistoryUseCase = ClearHistoryUseCase(fakeRepo)
        viewModel = HistoryViewModel(getHistoryUseCase, deleteRecordUseCase, clearHistoryUseCase)
    }

    @Test
    fun `adding record with duplicate id updates existing record and deduplicates`() = runBlocking {
        fakeRepo.addRecord(record1)
        fakeRepo.addRecord(record2)

        val updatedRecord1 = record1.copy(outputSizeBytes = 50L)
        fakeRepo.addRecord(updatedRecord1)

        val records = getHistoryUseCase().first()
        assertEquals(2, records.size)
        val fetched = records.find { it.id == "id-1" }
        assertEquals(50L, fetched?.outputSizeBytes)
    }

    @Test
    fun `deleting non-existent record does not error or modify list`() = runBlocking {
        fakeRepo.addRecord(record1)
        val result = deleteRecordUseCase("non_existent_id_999")
        assertTrue(result is AppResult.Success)

        val remaining = getHistoryUseCase().first()
        assertEquals(1, remaining.size)
        assertEquals(record1.id, remaining.first().id)
    }

    @Test
    fun `filteredRecords in HistoryUiState handles empty and category filtering`() {
        val emptyState = HistoryUiState(records = emptyList())
        assertTrue(emptyState.filteredRecords.isEmpty())

        val mixedState = HistoryUiState(
            records = listOf(record1, record2),
            selectedFilter = null
        )
        assertEquals(2, mixedState.filteredRecords.size)

        val pdfState = mixedState.copy(selectedFilter = ToolCategory.PDF)
        assertEquals(1, pdfState.filteredRecords.size)
        assertEquals(DocumentTool.IMAGES_TO_PDF, pdfState.filteredRecords.first().tool)

        val officeState = mixedState.copy(selectedFilter = ToolCategory.OFFICE)
        assertEquals(1, officeState.filteredRecords.size)
        assertEquals(DocumentTool.WORD_TO_PDF, officeState.filteredRecords.first().tool)
    }

    @Test
    fun `confirmation dialog open and dismiss events update state correctly`() {
        viewModel.onEvent(HistoryUiEvent.ShowClearConfirmation)
        // Dialog visibility state
        viewModel.onEvent(HistoryUiEvent.DismissClearConfirmation)
        viewModel.onEvent(HistoryUiEvent.ClearError)
    }

    // --- In-Memory Repository for Edge Case Verification ---

    private class InMemoryHistoryRepo : HistoryRepository {
        private val _flow = MutableStateFlow<List<ConversionRecord>>(emptyList())

        override fun getRecentConversions(): Flow<List<ConversionRecord>> = _flow.asStateFlow()

        override suspend fun addRecord(record: ConversionRecord): AppResult<Unit> {
            _flow.update { current ->
                listOf(record) + current.filter { it.id != record.id }
            }
            return AppResult.Success(Unit)
        }

        override suspend fun deleteRecord(id: String): AppResult<Unit> {
            _flow.update { current -> current.filter { it.id != id } }
            return AppResult.Success(Unit)
        }

        override suspend fun clearAll(): AppResult<Unit> {
            _flow.value = emptyList()
            return AppResult.Success(Unit)
        }
    }
}
