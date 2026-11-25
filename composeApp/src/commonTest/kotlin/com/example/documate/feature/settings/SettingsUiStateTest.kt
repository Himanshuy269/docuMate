package com.example.documate.feature.settings

import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.AppThemeMode
import com.example.documate.feature.settings.presentation.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsUiStateTest {

    @Test
    fun `default state is initialized with system defaults`() {
        val state = SettingsUiState()
        assertEquals(AppThemeMode.SYSTEM, state.themeMode)
        assertEquals(PdfPageSize.A4, state.defaultPageSize)
        assertEquals(CompressionPreset.MEDIUM, state.defaultCompression)
        assertEquals("http://10.0.2.2:8080", state.backendServiceUrl)
        assertEquals(0L, state.cacheSizeBytes)
        assertFalse(state.isConfirmClearCacheVisible)
        assertEquals(null, state.statusMessage)
    }

    @Test
    fun `formattedCacheSize formats 0 bytes correctly`() {
        val state = SettingsUiState(cacheSizeBytes = 0L)
        assertEquals("0 B", state.formattedCacheSize)
    }

    @Test
    fun `formattedCacheSize formats bytes correctly`() {
        val state = SettingsUiState(cacheSizeBytes = 500L)
        assertEquals("500 B", state.formattedCacheSize)
    }

    @Test
    fun `formattedCacheSize formats KB correctly`() {
        val stateRound = SettingsUiState(cacheSizeBytes = 51200L) // 50 KB
        assertEquals("50 KB", stateRound.formattedCacheSize)

        val stateFraction = SettingsUiState(cacheSizeBytes = 1536L) // 1.5 KB
        assertEquals("1.5 KB", stateFraction.formattedCacheSize)
    }

    @Test
    fun `formattedCacheSize formats MB correctly`() {
        val stateRound = SettingsUiState(cacheSizeBytes = 5242880L) // 5 MB
        assertEquals("5 MB", stateRound.formattedCacheSize)

        val stateFraction = SettingsUiState(cacheSizeBytes = 2621440L) // 2.5 MB
        assertEquals("2.5 MB", stateFraction.formattedCacheSize)
    }
}
