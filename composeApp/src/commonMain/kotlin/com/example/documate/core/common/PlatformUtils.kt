package com.example.documate.core.common

import kotlin.random.Random

/**
 * Multiplatform helper to get current epoch timestamp in milliseconds.
 */
expect fun currentTimeMillis(): Long

/**
 * Multiplatform helper to format a timestamp into a time string (e.g., "3:45 PM").
 */
expect fun formatTimestamp(timestampMillis: Long): String

/**
 * Universal pseudo-UUID generator formatted as 8-4-4-4-12 hex string.
 */
fun generateUuid(): String {
    fun hex(len: Int): String {
        val chars = "0123456789abcdef"
        return (1..len).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
    return "${hex(8)}-${hex(4)}-${hex(4)}-${hex(4)}-${hex(12)}"
}

/**
 * Formats a byte size into human-readable string (KB, MB, GB).
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var b = bytes.toDouble()
    var unitIndex = 0
    while (b >= 1024 && unitIndex < units.size - 1) {
        b /= 1024
        unitIndex++
    }
    val rounded = (b * 10).toLong() / 10.0
    return "$rounded ${units[unitIndex]}"
}
