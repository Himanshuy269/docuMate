package com.example.documate.core.data.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.AppThemeMode
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.UserSettings
import com.example.documate.core.domain.repository.SettingsRepository
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WebSettingsRepository : SettingsRepository {

    private val _settingsFlow = MutableStateFlow(readFromLocalStorage())

    override fun getSettings(): Flow<UserSettings> = _settingsFlow.asStateFlow()

    override fun getSettingsSync(): UserSettings = _settingsFlow.value

    override suspend fun updateTheme(themeMode: AppThemeMode): AppResult<Unit> {
        localStorage.setItem(KEY_THEME, themeMode.name)
        _settingsFlow.update { it.copy(themeMode = themeMode) }
        return AppResult.Success(Unit)
    }

    override suspend fun updateDefaultPageSize(pageSize: PdfPageSize): AppResult<Unit> {
        localStorage.setItem(KEY_PAGE_SIZE, pageSize.name)
        _settingsFlow.update { it.copy(defaultPageSize = pageSize) }
        return AppResult.Success(Unit)
    }

    override suspend fun updateDefaultCompression(preset: CompressionPreset): AppResult<Unit> {
        localStorage.setItem(KEY_COMPRESSION, preset.name)
        _settingsFlow.update { it.copy(defaultCompression = preset) }
        return AppResult.Success(Unit)
    }

    override suspend fun updateBackendUrl(url: String): AppResult<Unit> {
        localStorage.setItem(KEY_BACKEND_URL, url)
        _settingsFlow.update { it.copy(backendServiceUrl = url) }
        return AppResult.Success(Unit)
    }

    override suspend fun getAppCacheSizeBytes(): Long = 0L

    override suspend fun clearAppCache(): AppResult<Long> = AppResult.Success(0L)

    private fun readFromLocalStorage(): UserSettings {
        val themeStr = localStorage.getItem(KEY_THEME) ?: AppThemeMode.SYSTEM.name
        val pageSizeStr = localStorage.getItem(KEY_PAGE_SIZE) ?: PdfPageSize.A4.name
        val compStr = localStorage.getItem(KEY_COMPRESSION) ?: CompressionPreset.MEDIUM.name
        val url = localStorage.getItem(KEY_BACKEND_URL) ?: "http://localhost:8080"

        val theme = try { AppThemeMode.valueOf(themeStr) } catch (_: Exception) { AppThemeMode.SYSTEM }
        val size = try { PdfPageSize.valueOf(pageSizeStr) } catch (_: Exception) { PdfPageSize.A4 }
        val comp = try { CompressionPreset.valueOf(compStr) } catch (_: Exception) { CompressionPreset.MEDIUM }

        return UserSettings(
            themeMode = theme,
            defaultPageSize = size,
            defaultCompression = comp,
            backendServiceUrl = url
        )
    }

    companion object {
        private const val KEY_THEME = "documate_theme"
        private const val KEY_PAGE_SIZE = "documate_page_size"
        private const val KEY_COMPRESSION = "documate_compression"
        private const val KEY_BACKEND_URL = "documate_backend_url"
    }
}
