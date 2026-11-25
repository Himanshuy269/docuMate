package com.example.documate.feature.history

import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.feature.history.presentation.HistoryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryUiStateTest {

    private val rec1 = ConversionRecord(
        id = "1",
        tool = DocumentTool.IMAGES_TO_PDF,
        sourceFileName = "img.png",
        outputFileName = "img.pdf",
        outputPath = "/path/1",
        originalSizeBytes = 1000L,
        outputSizeBytes = 800L,
        timestampMillis = 100L
    )

    private val rec2 = ConversionRecord(
        id = "2",
        tool = DocumentTool.EXCEL_TO_PDF,
        sourceFileName = "sheet.xlsx",
        outputFileName = "sheet.pdf",
        outputPath = "/path/2",
        originalSizeBytes = 2000L,
        outputSizeBytes = 1500L,
        timestampMillis = 200L
    )

    @Test
    fun `default state is initialized correctly`() {
        val state = HistoryUiState()
        assertTrue(state.records.isEmpty())
        assertTrue(state.filteredRecords.isEmpty())
        assertEquals(null, state.selectedFilter)
        assertFalse(state.isConfirmClearDialogOpen)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `filtering by category returns correct subset`() {
        val state = HistoryUiState(
            records = listOf(rec1, rec2),
            selectedFilter = ToolCategory.OFFICE
        )
        assertEquals(2, state.records.size)
        assertEquals(1, state.filteredRecords.size)
        assertEquals(DocumentTool.EXCEL_TO_PDF, state.filteredRecords[0].tool)
    }

    @Test
    fun `null filter returns all records in filteredRecords`() {
        val state = HistoryUiState(
            records = listOf(rec1, rec2),
            selectedFilter = null
        )
        assertEquals(2, state.filteredRecords.size)
    }
}
