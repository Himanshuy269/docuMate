package com.example.documate.feature.home

import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.feature.home.presentation.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateEdgeCaseTest {

    @Test
    fun `search with leading and trailing whitespace matches correctly`() {
        val state = HomeUiState(searchQuery = "   word   ")
        val tools = state.filteredTools

        assertEquals(1, tools.size)
        assertEquals(DocumentTool.WORD_TO_PDF, tools.first())
    }

    @Test
    fun `search query is case insensitive`() {
        val stateUpper = HomeUiState(searchQuery = "EXCEL")
        assertEquals(1, stateUpper.filteredTools.size)
        assertEquals(DocumentTool.EXCEL_TO_PDF, stateUpper.filteredTools.first())

        val stateMixed = HomeUiState(searchQuery = "pOWerPOint")
        assertEquals(1, stateMixed.filteredTools.size)
        assertEquals(DocumentTool.POWERPOINT_TO_PDF, stateMixed.filteredTools.first())
    }

    @Test
    fun `blank search query returns all tools`() {
        val state = HomeUiState(searchQuery = "     ")
        assertEquals(DocumentTool.entries.size, state.filteredTools.size)
    }

    @Test
    fun `search with no matches returns empty list`() {
        val state = HomeUiState(searchQuery = "xyznonexistenttool123")
        assertTrue(state.filteredTools.isEmpty())
        assertTrue(state.pdfTools.isEmpty())
        assertTrue(state.officeTools.isEmpty())
    }

    @Test
    fun `category filter combined with search query`() {
        // Search "to pdf" under OFFICE category
        val state = HomeUiState(
            searchQuery = "to pdf",
            selectedCategory = ToolCategory.OFFICE
        )
        assertEquals(3, state.filteredTools.size)
        assertTrue(state.filteredTools.all { it.category == ToolCategory.OFFICE })

        // Search "compression" under OFFICE category (should return none)
        val stateNoMatch = HomeUiState(
            searchQuery = "compression",
            selectedCategory = ToolCategory.OFFICE
        )
        assertTrue(stateNoMatch.filteredTools.isEmpty())
    }
}
