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
    val outputFilePath: String get() = outputPath
    val outputUriString: String get() = outputPath
    val inputSizeBytes: Long get() = originalSizeBytes

    val formattedOutputSize: String
        get() {
            val mbRaw = outputSizeBytes / (1024.0 * 1024.0)
            if (mbRaw >= 1.0) {
                val mb = kotlin.math.round(mbRaw * 10.0) / 10.0
                val mbStr = if (mb.toLong().toDouble() == mb) "${mb.toLong()}.0" else "$mb"
                return "$mbStr MB"
            }
            return "${(outputSizeBytes / 1024).coerceAtLeast(1)} KB"
        }

    val compressionRatioText: String?
        get() {
            if (originalSizeBytes <= 0 || outputSizeBytes >= originalSizeBytes) return null
            val saved = kotlin.math.round(((originalSizeBytes - outputSizeBytes).toDouble() / originalSizeBytes) * 100).toLong()
            return "-$saved%"
        }
}
