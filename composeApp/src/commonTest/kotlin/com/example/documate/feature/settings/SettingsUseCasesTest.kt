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
import com.example.documate.core.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsUseCasesTest {

    private lateinit var fakeRepo: FakeSettingsRepository
    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var updateThemeUseCase: UpdateThemeUseCase
    private lateinit var updatePageSizeUseCase: UpdateDefaultPageSizeUseCase
    private lateinit var updateCompressionUseCase: UpdateDefaultCompressionUseCase
    private lateinit var updateBackendUrlUseCase: UpdateBackendUrlUseCase
    private lateinit var manageCacheUseCase: ManageAppCacheUseCase

    @Before
    fun setup() {
        fakeRepo = FakeSettingsRepository()
        getSettingsUseCase = GetSettingsUseCase(fakeRepo)
        updateThemeUseCase = UpdateThemeUseCase(fakeRepo)
        updatePageSizeUseCase = UpdateDefaultPageSizeUseCase(fakeRepo)
        updateCompressionUseCase = UpdateDefaultCompressionUseCase(fakeRepo)
        updateBackendUrlUseCase = UpdateBackendUrlUseCase(fakeRepo)
        manageCacheUseCase = ManageAppCacheUseCase(fakeRepo)
    }

    @Test
    fun `getSettings returns default settings initial state`() = runBlocking {
        val settings = getSettingsUseCase().first()
        assertEquals(AppThemeMode.SYSTEM, settings.themeMode)
        assertEquals(PdfPageSize.A4, settings.defaultPageSize)
        assertEquals(CompressionPreset.MEDIUM, settings.defaultCompression)
        assertEquals("http://10.0.2.2:8080", settings.backendServiceUrl)
    }

    @Test
    fun `updateTheme updates theme preference`() = runBlocking {
        val result = updateThemeUseCase(AppThemeMode.DARK)
        assertTrue(result is AppResult.Success)

        val updated = getSettingsUseCase().first()
        assertEquals(AppThemeMode.DARK, updated.themeMode)
    }

    @Test
    fun `updateDefaultPageSize updates size preference`() = runBlocking {
        val result = updatePageSizeUseCase(PdfPageSize.LETTER)
        assertTrue(result is AppResult.Success)

        val updated = getSettingsUseCase().first()
        assertEquals(PdfPageSize.LETTER, updated.defaultPageSize)
    }

    @Test
    fun `updateDefaultCompression updates compression preference`() = runBlocking {
        val result = updateCompressionUseCase(CompressionPreset.HIGH)
        assertTrue(result is AppResult.Success)

        val updated = getSettingsUseCase().first()
        assertEquals(CompressionPreset.HIGH, updated.defaultCompression)
    }

    @Test
    fun `updateBackendUrl with valid HTTPS url succeeds`() = runBlocking {
        val result = updateBackendUrlUseCase("https://converter.mycorp.internal:8443")
        assertTrue(result is AppResult.Success)

        val updated = getSettingsUseCase().first()
        assertEquals("https://converter.mycorp.internal:8443", updated.backendServiceUrl)
    }

    @Test
    fun `updateBackendUrl with blank url returns error`() = runBlocking {
        val result = updateBackendUrlUseCase("   ")
        assertTrue(result is AppResult.Error)
        assertEquals("Backend URL cannot be blank", (result as AppResult.Error).message)
    }

    @Test
    fun `updateBackendUrl with missing scheme returns error`() = runBlocking {
        val result = updateBackendUrlUseCase("ftp://invalid.com")
        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("http:// or https://"))
    }

    @Test
    fun `manageCache get and clear cache operations work correctly`() = runBlocking {
        fakeRepo.simulatedCacheBytes = 1048576L // 1 MB
        val sizeBefore = manageCacheUseCase.getCacheSizeBytes()
        assertEquals(1048576L, sizeBefore)

        val clearResult = manageCacheUseCase.clearCache()
        assertTrue(clearResult is AppResult.Success)
        assertEquals(1048576L, (clearResult as AppResult.Success).data)

        val sizeAfter = manageCacheUseCase.getCacheSizeBytes()
        assertEquals(0L, sizeAfter)
    }

    private class FakeSettingsRepository : SettingsRepository {
        private var currentSettings = UserSettings()
        private val _flow = MutableStateFlow(currentSettings)
        var simulatedCacheBytes = 0L

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
            currentSettings = currentSettings.copy(backendServiceUrl = url)
            _flow.value = currentSettings
            return AppResult.Success(Unit)
        }

        override suspend fun getAppCacheSizeBytes(): Long = simulatedCacheBytes

        override suspend fun clearAppCache(): AppResult<Long> {
            val freed = simulatedCacheBytes
            simulatedCacheBytes = 0L
            return AppResult.Success(freed)
        }
    }
}
