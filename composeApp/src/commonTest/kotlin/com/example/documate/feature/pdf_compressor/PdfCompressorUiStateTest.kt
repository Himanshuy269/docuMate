package com.example.documate.feature.pdf_compressor

import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.feature.pdf_compressor.presentation.PdfCompressorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompressorUiStateTest {

    private val samplePdf = DocumentFile(
        id = "1",
        name = "test.pdf",
        uriString = "content://docs/test.pdf",
        sizeBytes = 5000000L,
        mimeType = "application/pdf",
        extension = "pdf"
    )

    @Test
    fun `initial state defaults are correct`() {
        val state = PdfCompressorUiState()

        assertEquals(null, state.selectedPdf)
        assertEquals(0, state.pdfPageCount)
        assertEquals(CompressionPreset.MEDIUM, state.selectedPreset)
        assertEquals("", state.outputFileName)
        assertFalse(state.isCompressing)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals(null, state.resultFile)
        assertEquals(null, state.errorMessage)
        assertFalse(state.canCompress)
    }

    @Test
    fun `canCompress returns false when pdf is null`() {
        val state = PdfCompressorUiState(
            selectedPdf = null,
            outputFileName = "Compressed.pdf"
        )
        assertFalse(state.canCompress)
    }

    @Test
    fun `canCompress returns false when output name is blank`() {
        val state = PdfCompressorUiState(
            selectedPdf = samplePdf,
            outputFileName = "   "
        )
        assertFalse(state.canCompress)
    }

    @Test
    fun `canCompress returns false while compressing`() {
        val state = PdfCompressorUiState(
            selectedPdf = samplePdf,
            outputFileName = "Compressed.pdf",
            isCompressing = true
        )
        assertFalse(state.canCompress)
    }

    @Test
    fun `canCompress returns true when pdf is set and not compressing`() {
        val state = PdfCompressorUiState(
            selectedPdf = samplePdf,
            outputFileName = "Compressed.pdf",
            isCompressing = false
        )
        assertTrue(state.canCompress)
    }

    @Test
    fun `compression presets configuration matches requirements`() {
        assertEquals("Low Compression", CompressionPreset.LOW.title)
        assertEquals(85, CompressionPreset.LOW.targetQualityPercent)
        assertEquals(200, CompressionPreset.LOW.imageDpi)

        assertEquals("Recommended", CompressionPreset.MEDIUM.title)
        assertEquals(65, CompressionPreset.MEDIUM.targetQualityPercent)
        assertEquals(150, CompressionPreset.MEDIUM.imageDpi)

        assertEquals("High Compression", CompressionPreset.HIGH.title)
        assertEquals(40, CompressionPreset.HIGH.targetQualityPercent)
        assertEquals(96, CompressionPreset.HIGH.imageDpi)
    }
}
