package com.example.documate.core

import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversionRecordTest {

    @Test
    fun `compressionRatioText with 0 original bytes returns null`() {
        val record = ConversionRecord(
            id = "1",
            tool = DocumentTool.PDF_COMPRESSION,
            sourceFileName = "test.pdf",
            outputFileName = "test_compressed.pdf",
            outputPath = "/path/test_compressed.pdf",
            originalSizeBytes = 0L,
            outputSizeBytes = 100L,
            timestampMillis = 1000L
        )
        assertNull(record.compressionRatioText)
    }

    @Test
    fun `compressionRatioText when output size is greater than or equal to original returns null`() {
        val recordLarger = ConversionRecord(
            id = "2",
            tool = DocumentTool.PDF_COMPRESSION,
            sourceFileName = "test.pdf",
            outputFileName = "test_compressed.pdf",
            outputPath = "/path/test_compressed.pdf",
            originalSizeBytes = 1000L,
            outputSizeBytes = 1200L,
            timestampMillis = 1000L
        )
        assertNull(recordLarger.compressionRatioText)

        val recordEqual = recordLarger.copy(outputSizeBytes = 1000L)
        assertNull(recordEqual.compressionRatioText)
    }

    @Test
    fun `compressionRatioText when compressed computes percentage reduction`() {
        val record = ConversionRecord(
            id = "3",
            tool = DocumentTool.PDF_COMPRESSION,
            sourceFileName = "test.pdf",
            outputFileName = "test_compressed.pdf",
            outputPath = "/path/test_compressed.pdf",
            originalSizeBytes = 1000L,
            outputSizeBytes = 500L,
            timestampMillis = 1000L
        )
        assertEquals("-50%", record.compressionRatioText)
    }

    @Test
    fun `compressionRatioText with high reduction computes correctly`() {
        val record = ConversionRecord(
            id = "4",
            tool = DocumentTool.PDF_COMPRESSION,
            sourceFileName = "test.pdf",
            outputFileName = "test_compressed.pdf",
            outputPath = "/path/test_compressed.pdf",
            originalSizeBytes = 10000L,
            outputSizeBytes = 2500L,
            timestampMillis = 1000L
        )
        assertEquals("-75%", record.compressionRatioText)
    }
}
