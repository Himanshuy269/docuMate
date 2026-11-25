package com.example.documate.core.domain.model

/**
 * Domain entity representing a selected document or image file.
 * Completely decoupled from Android URIs and file system details.
 */
data class DocumentFile(
    val id: String,
    val name: String,
    val uriString: String,
    val sizeBytes: Long,
    val mimeType: String = "",
    val extension: String = ""
) {
    /**
     * Formats bytes into a human-readable string (e.g. 1.2 MB, 450 KB).
     */
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var size = sizeBytes.toDouble()
            var unitIndex = 0
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            return if (unitIndex == 0) {
                "${size.toLong()} ${units[unitIndex]}"
            } else {
                val rounded = kotlin.math.round(size * 10.0) / 10.0
                val str = if (rounded.toLong().toDouble() == rounded) "${rounded.toLong()}.0" else "$rounded"
                "$str ${units[unitIndex]}"
            }
        }
}
