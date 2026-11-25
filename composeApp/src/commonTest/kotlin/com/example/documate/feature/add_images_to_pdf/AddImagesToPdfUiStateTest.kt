package com.example.documate.feature.add_images_to_pdf

import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.feature.add_images_to_pdf.presentation.AddImagesToPdfUiState
import com.example.documate.feature.add_images_to_pdf.presentation.InsertPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddImagesToPdfUiStateTest {

    private val samplePdf = DocumentFile(
        id = "1",
        name = "test.pdf",
        uriString = "content://docs/test.pdf",
        sizeBytes = 1000L,
        mimeType = "application/pdf",
        extension = "pdf"
    )

    private val sampleImage = DocumentFile(
        id = "2",
        name = "test.jpg",
        uriString = "content://media/test.jpg",
        sizeBytes = 500L,
        mimeType = "image/jpeg",
        extension = "jpg"
    )

    @Test
    fun `initial state defaults are correct`() {
        val state = AddImagesToPdfUiState()

        assertEquals(null, state.selectedPdf)
        assertEquals(0, state.pdfPageCount)
        assertTrue(state.selectedImages.isEmpty())
        assertEquals(InsertPosition.AT_END, state.insertPosition)
        assertEquals(1, state.specificPageNumber)
        assertFalse(state.isProcessing)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals(null, state.resultFile)
        assertEquals(null, state.errorMessage)
        assertFalse(state.canProcess)
    }

    @Test
    fun `canProcess returns false when pdf is missing`() {
        val state = AddImagesToPdfUiState(
            selectedPdf = null,
            selectedImages = listOf(sampleImage),
            outputFileName = "Output"
        )
        assertFalse(state.canProcess)
    }

    @Test
    fun `canProcess returns false when images list is empty`() {
        val state = AddImagesToPdfUiState(
            selectedPdf = samplePdf,
            selectedImages = emptyList(),
            outputFileName = "Output"
        )
        assertFalse(state.canProcess)
    }

    @Test
    fun `canProcess returns false when output file name is blank`() {
        val state = AddImagesToPdfUiState(
            selectedPdf = samplePdf,
            selectedImages = listOf(sampleImage),
            outputFileName = "   "
        )
        assertFalse(state.canProcess)
    }

    @Test
    fun `canProcess returns false while processing`() {
        val state = AddImagesToPdfUiState(
            selectedPdf = samplePdf,
            selectedImages = listOf(sampleImage),
            outputFileName = "Output",
            isProcessing = true
        )
        assertFalse(state.canProcess)
    }

    @Test
    fun `canProcess returns true when all requirements are met`() {
        val state = AddImagesToPdfUiState(
            selectedPdf = samplePdf,
            selectedImages = listOf(sampleImage),
            outputFileName = "Output",
            isProcessing = false
        )
        assertTrue(state.canProcess)
    }

    @Test
    fun `insert position enum properties are correct`() {
        assertEquals("At the beginning", InsertPosition.AT_BEGINNING.title)
        assertEquals("At the end", InsertPosition.AT_END.title)
        assertEquals("After specific page", InsertPosition.SPECIFIC_PAGE.title)
    }
}
