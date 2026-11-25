package com.example.documate.feature.settings

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.UserSettings
import com.example.documate.core.domain.repository.SettingsRepository
import com.example.documate.feature.settings.domain.usecase.GetSettingsUseCase
import com.example.documate.feature.settings.domain.usecase.ManageAppCacheUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateBackendUrlUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateDefaultCompressionUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateDefaultPageSizeUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateThemeUseCase
import com.example.documate.feature.settings.presentation.AppThemeMode
import com.example.documate.feature.settings.presentation.SettingsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsEdgeCasesTest {

    private lateinit var fakeRepo: MockSettingsRepository
    private lateinit var updateBackendUrlUseCase: UpdateBackendUrlUseCase
    private lateinit var manageCacheUseCase: ManageAppCacheUseCase

    @Before
    fun setup() {
        fakeRepo = MockSettingsRepository()
        updateBackendUrlUseCase = UpdateBackendUrlUseCase(fakeRepo)
        manageCacheUseCase = ManageAppCacheUseCase(fakeRepo)
    }

    @Test
    fun `updateBackendUrl rejects unsupported schemes`() = runBlocking {
        val ftpResult = updateBackendUrlUseCase("ftp://192.168.1.10:8080")
        assertTrue(ftpResult is AppResult.Error)
        assertEquals("URL must start with http:// or https://", (ftpResult as AppResult.Error).message)

        val fileResult = updateBackendUrlUseCase("file:///sdcard/server")
        assertTrue(fileResult is AppResult.Error)

        val noSchemeResult = updateBackendUrlUseCase("10.0.2.2:8080")
        assertTrue(noSchemeResult is AppResult.Error)
    }

    @Test
    fun `updateBackendUrl accepts valid HTTP and HTTPS urls with trimming`() = runBlocking {
        val httpResult = updateBackendUrlUseCase("   http://192.168.1.50:8080   ")
        assertTrue(httpResult is AppResult.Success)
        assertEquals("http://192.168.1.50:8080", fakeRepo.currentSettings.backendServiceUrl)

        val httpsResult = updateBackendUrlUseCase("https://api.documate.local/v1")
        assertTrue(httpsResult is AppResult.Success)
        assertEquals("https://api.documate.local/v1", fakeRepo.currentSettings.backendServiceUrl)
    }

    @Test
    fun `manageCacheUseCase returns 0 when cache is empty`() = runBlocking {
        fakeRepo.cacheBytes = 0L
        val size = manageCacheUseCase.getCacheSizeBytes()
        assertEquals(0L, size)

        val clearResult = manageCacheUseCase.clearCache()
        assertTrue(clearResult is AppResult.Success)
        assertEquals(0L, (clearResult as AppResult.Success).data)
    }

    @Test
    fun `manageCacheUseCase clears non-empty cache correctly`() = runBlocking {
        fakeRepo.cacheBytes = 15485760L // ~15 MB
        val size = manageCacheUseCase.getCacheSizeBytes()
        assertEquals(15485760L, size)

        val clearResult = manageCacheUseCase.clearCache()
        assertTrue(clearResult is AppResult.Success)
        assertEquals(15485760L, (clearResult as AppResult.Success).data)
        assertEquals(0L, manageCacheUseCase.getCacheSizeBytes())
    }

    @Test
    fun `settingsUiState holds default values correctly`() {
        val state = SettingsUiState()
        assertEquals(AppThemeMode.SYSTEM, state.themeMode)
        assertEquals(PdfPageSize.A4, state.defaultPageSize)
        assertEquals(CompressionPreset.MEDIUM, state.defaultCompression)
        assertEquals("http://10.0.2.2:8080", state.backendServiceUrl)
        assertEquals(0L, state.cacheSizeBytes)
    }

    // --- Mock Settings Repo ---

    private class MockSettingsRepository : SettingsRepository {
        var currentSettings = UserSettings()
        var cacheBytes = 0L

        private val _flow = MutableStateFlow(currentSettings)
        override fun getSettings(): Flow<UserSettings> = _flow.asStateFlow()
        override fun getSettingsSync(): UserSettings = currentSettings

        override suspend fun updateTheme(themeMode: AppThemeMode): AppResult<Unit> {
            currentSettings = currentSettings.copy(themeMode = themeMode)
            _flow.value = currentSettings
            return AppResult.Success(Unit)
        }

        override suspend fun updateDefaultPageSize(pageSize: PdfPageSize): AppResult<Unit> {
            currentSettings = currentSettings.copy(defaultPageSize = pageSize)
            _flow.value = currentSettings
            return AppResult.Success(Unit)
        }

        override suspend fun updateDefaultCompression(preset: CompressionPreset): AppResult<Unit> {
            currentSettings = currentSettings.copy(defaultCompression = preset)
            _flow.value = currentSettings
            return AppResult.Success(Unit)
        }

        override suspend fun updateBackendUrl(url: String): AppResult<Unit> {
            val trimmed = url.trim()
            if (trimmed.isBlank()) return AppResult.Error("Backend URL cannot be blank")
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                return AppResult.Error("URL must start with http:// or https://")
            }
            currentSettings = currentSettings.copy(backendServiceUrl = trimmed)
            _flow.value = currentSettings
            return AppResult.Success(Unit)
        }

        override suspend fun getAppCacheSizeBytes(): Long = cacheBytes

        override suspend fun clearAppCache(): AppResult<Long> {
            val freed = cacheBytes
            cacheBytes = 0L
            return AppResult.Success(freed)
        }
    }
}
