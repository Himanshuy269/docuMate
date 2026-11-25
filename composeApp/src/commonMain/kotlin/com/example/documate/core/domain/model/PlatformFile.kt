package com.example.documate.core.domain.model

data class PlatformFile(
    val path: String,
    val name: String = "",
    val sizeBytes: Long = 0L
) {
    val nameWithoutExtension: String
        get() = name.substringBeforeLast('.', name)
}
