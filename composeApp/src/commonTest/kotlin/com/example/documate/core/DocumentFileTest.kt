package com.example.documate.core

import com.example.documate.core.domain.model.DocumentFile
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentFileTest {

    @Test
    fun `formattedSize with 0 bytes returns 0 B`() {
        val file = DocumentFile(
            id = "test-1",
            name = "empty.txt",
            uriString = "content://media/1",
            sizeBytes = 0L
        )
        assertEquals("0 B", file.formattedSize)
    }

    @Test
    fun `formattedSize with negative bytes returns 0 B`() {
        val file = DocumentFile(
            id = "test-2",
            name = "neg.txt",
            uriString = "content://media/2",
            sizeBytes = -50L
        )
        assertEquals("0 B", file.formattedSize)
    }

    @Test
    fun `formattedSize with bytes below 1024 returns B`() {
        val file = DocumentFile(
            id = "test-3",
            name = "small.txt",
            uriString = "content://media/3",
            sizeBytes = 512L
        )
        assertEquals("512 B", file.formattedSize)
    }

    @Test
    fun `formattedSize with 1024 bytes returns KB`() {
        val file = DocumentFile(
            id = "test-4",
            name = "one_kb.txt",
            uriString = "content://media/4",
            sizeBytes = 1024L
        )
        assertEquals("1.0 KB", file.formattedSize)
    }

    @Test
    fun `formattedSize with megabytes returns MB`() {
        val file = DocumentFile(
            id = "test-5",
            name = "large.pdf",
            uriString = "content://media/5",
            sizeBytes = 2500000L
        )
        // 2500000 / (1024 * 1024) = 2.384...
        assertEquals("2.4 MB", file.formattedSize)
    }

    @Test
    fun `formattedSize with gigabytes returns GB`() {
        val file = DocumentFile(
            id = "test-6",
            name = "huge.bin",
            uriString = "content://media/6",
            sizeBytes = 3221225472L // 3 GB
        )
        assertEquals("3.0 GB", file.formattedSize)
    }
}
