package com.example.documate.core.domain.model

/**
 * Domain entity representing a completed document conversion record.
 */
data class ConversionRecord(
    val id: String,
    val tool: DocumentTool,
    val sourceFileName: String,
    val outputFileName: String,
    val outputPath: String,
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val timestampMillis: Long
) {
    val compressionRatioText: String?
        get() {
            if (originalSizeBytes <= 0 || outputSizeBytes >= originalSizeBytes) return null
            val saved = ((originalSizeBytes - outputSizeBytes).toDouble() / originalSizeBytes) * 100
            return String.format("-%.0f%%", saved)
        }
}
