package com.example.documate.core.domain.model

import java.util.Locale

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
    val outputFilePath: String get() = outputPath
    val outputUriString: String get() = outputPath
    val inputSizeBytes: Long get() = originalSizeBytes

    val formattedOutputSize: String
        get() {
            val mb = outputSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) String.format(Locale.getDefault(), "%.1f MB", mb)
            else "${(outputSizeBytes / 1024).coerceAtLeast(1)} KB"
        }

    val compressionRatioText: String?
        get() {
            if (originalSizeBytes <= 0 || outputSizeBytes >= originalSizeBytes) return null
            val saved = ((originalSizeBytes - outputSizeBytes).toDouble() / originalSizeBytes) * 100
            return String.format(Locale.getDefault(), "-%.0f%%", saved)
        }
}
