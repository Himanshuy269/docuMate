package com.example.documate.feature.images_to_pdf

import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.feature.images_to_pdf.presentation.ImagesToPdfUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagesToPdfUiStateTest {

    @Test
    fun `default state cannot convert`() {
        val state = ImagesToPdfUiState()
        assertFalse("Default state with no images should not be convertible", state.canConvert)
        assertEquals(PdfPageSize.A4, state.pageSize)
        assertEquals(PdfOrientation.PORTRAIT, state.orientation)
        assertEquals(PdfMargin.NONE, state.margin)
        assertFalse(state.isProcessing)
    }

    @Test
    fun `state with selected images and non-blank output name can convert`() {
        val testImages = listOf(
            DocumentFile(
                id = "1",
                name = "img1.png",
                uriString = "content://images/1",
                sizeBytes = 1000L,
                mimeType = "image/png",
                extension = "png"
            )
        )
        val state = ImagesToPdfUiState(
            selectedImages = testImages,
            outputFileName = "MyScan"
        )
        assertTrue("State with images and output name should be convertible", state.canConvert)
    }

    @Test
    fun `state when processing cannot convert`() {
        val testImages = listOf(
            DocumentFile(
                id = "1",
                name = "img1.png",
                uriString = "content://images/1",
                sizeBytes = 1000L,
                mimeType = "image/png",
                extension = "png"
            )
        )
        val state = ImagesToPdfUiState(
            selectedImages = testImages,
            outputFileName = "MyScan",
            isProcessing = true
        )
        assertFalse("State should not be convertible while processing", state.canConvert)
    }

    @Test
    fun `state with blank output name cannot convert`() {
        val testImages = listOf(
            DocumentFile(
                id = "1",
                name = "img1.png",
                uriString = "content://images/1",
                sizeBytes = 1000L,
                mimeType = "image/png",
                extension = "png"
            )
        )
        val state = ImagesToPdfUiState(
            selectedImages = testImages,
            outputFileName = "   "
        )
        assertFalse("State with blank file name should not be convertible", state.canConvert)
    }
}
