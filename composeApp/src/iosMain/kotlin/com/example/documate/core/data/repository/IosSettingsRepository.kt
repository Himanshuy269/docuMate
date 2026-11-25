package com.example.documate.core.data.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.AppThemeMode
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.UserSettings
import com.example.documate.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSUserDefaults

class IosSettingsRepository : SettingsRepository {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val _settingsFlow = MutableStateFlow(readFromDefaults())

    override fun getSettings(): Flow<UserSettings> = _settingsFlow.asStateFlow()

    override fun getSettingsSync(): UserSettings = _settingsFlow.value

    override suspend fun updateTheme(themeMode: AppThemeMode): AppResult<Unit> {
        defaults.setObject(themeMode.name, forKey = KEY_THEME)
        _settingsFlow.update { it.copy(themeMode = themeMode) }
        return AppResult.Success(Unit)
    }

    override suspend fun updateDefaultPageSize(pageSize: PdfPageSize): AppResult<Unit> {
        defaults.setObject(pageSize.name, forKey = KEY_PAGE_SIZE)
        _settingsFlow.update { it.copy(defaultPageSize = pageSize) }
        return AppResult.Success(Unit)
    }

    override suspend fun updateDefaultCompression(preset: CompressionPreset): AppResult<Unit> {
        defaults.setObject(preset.name, forKey = KEY_COMPRESSION)
        _settingsFlow.update { it.copy(defaultCompression = preset) }
        return AppResult.Success(Unit)
    }

    override suspend fun updateBackendUrl(url: String): AppResult<Unit> {
        defaults.setObject(url, forKey = KEY_BACKEND_URL)
        _settingsFlow.update { it.copy(backendServiceUrl = url) }
        return AppResult.Success(Unit)
    }

    override suspend fun getAppCacheSizeBytes(): Long {
        return 0L
    }

    override suspend fun clearAppCache(): AppResult<Long> {
        return AppResult.Success(0L)
    }

    private fun readFromDefaults(): UserSettings {
        val themeStr = defaults.stringForKey(KEY_THEME) ?: AppThemeMode.SYSTEM.name
        val pageSizeStr = defaults.stringForKey(KEY_PAGE_SIZE) ?: PdfPageSize.A4.name
        val compStr = defaults.stringForKey(KEY_COMPRESSION) ?: CompressionPreset.MEDIUM.name
        val url = defaults.stringForKey(KEY_BACKEND_URL) ?: "http://localhost:8080"

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
