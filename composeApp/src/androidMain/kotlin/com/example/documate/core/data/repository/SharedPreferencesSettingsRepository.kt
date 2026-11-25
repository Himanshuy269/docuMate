package com.example.documate.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.UserSettings
import com.example.documate.core.domain.repository.SettingsRepository
import com.example.documate.core.domain.model.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android SharedPreferences-backed implementation of SettingsRepository.
 * Manages persisted user configurations and cache pruning.
 */
class SharedPreferencesSettingsRepository(
    private val context: Context,
    private val prefsName: String = "documate_user_settings"
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(readFromPrefs())

    override fun getSettings(): Flow<UserSettings> = _settingsFlow.asStateFlow()

    override fun getSettingsSync(): UserSettings = _settingsFlow.value

    override suspend fun updateTheme(themeMode: AppThemeMode): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putString(KEY_THEME, themeMode.name).apply()
            _settingsFlow.update { it.copy(themeMode = themeMode) }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to save theme setting: ${e.message}", e)
        }
    }

    override suspend fun updateDefaultPageSize(pageSize: PdfPageSize): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putString(KEY_PAGE_SIZE, pageSize.name).apply()
            _settingsFlow.update { it.copy(defaultPageSize = pageSize) }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to save default page size: ${e.message}", e)
        }
    }

    override suspend fun updateDefaultCompression(preset: CompressionPreset): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putString(KEY_COMPRESSION, preset.name).apply()
            _settingsFlow.update { it.copy(defaultCompression = preset) }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to save default compression preset: ${e.message}", e)
        }
    }

    override suspend fun updateBackendUrl(url: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trim()
            if (cleanUrl.isBlank()) {
                return@withContext AppResult.Error("Backend URL cannot be blank")
            }
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                return@withContext AppResult.Error("URL must start with http:// or https://")
            }
            prefs.edit().putString(KEY_BACKEND_URL, cleanUrl).apply()
            _settingsFlow.update { it.copy(backendServiceUrl = cleanUrl) }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to save backend URL: ${e.message}", e)
        }
    }

    override suspend fun getAppCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        try {
            var totalBytes = 0L
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                totalBytes += cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
            val officeDir = File(context.filesDir, "office_pdfs")
            if (officeDir.exists()) {
                totalBytes += officeDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
            totalBytes
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun clearAppCache(): AppResult<Long> = withContext(Dispatchers.IO) {
        try {
            var freedBytes = 0L
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    freedBytes += file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    file.deleteRecursively()
                }
            }
            val officeDir = File(context.filesDir, "office_pdfs")
            if (officeDir.exists()) {
                officeDir.listFiles()?.forEach { file ->
                    freedBytes += file.length()
                    file.delete()
                }
            }
            AppResult.Success(freedBytes)
        } catch (e: Exception) {
            AppResult.Error("Failed to clear application cache: ${e.message}", e)
        }
    }

    private fun readFromPrefs(): UserSettings {
        val themeStr = prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name)
        val theme = runCatching { AppThemeMode.valueOf(themeStr ?: AppThemeMode.SYSTEM.name) }.getOrDefault(AppThemeMode.SYSTEM)

        val sizeStr = prefs.getString(KEY_PAGE_SIZE, PdfPageSize.A4.name)
        val size = runCatching { PdfPageSize.valueOf(sizeStr ?: PdfPageSize.A4.name) }.getOrDefault(PdfPageSize.A4)

        val compStr = prefs.getString(KEY_COMPRESSION, CompressionPreset.MEDIUM.name)
        val compression = runCatching { CompressionPreset.valueOf(compStr ?: CompressionPreset.MEDIUM.name) }.getOrDefault(CompressionPreset.MEDIUM)

        val backendUrl = prefs.getString(KEY_BACKEND_URL, "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"

        return UserSettings(
            themeMode = theme,
            defaultPageSize = size,
            defaultCompression = compression,
            backendServiceUrl = backendUrl
        )
    }

    companion object {
        private const val KEY_THEME = "key_theme_mode"
        private const val KEY_PAGE_SIZE = "key_default_page_size"
        private const val KEY_COMPRESSION = "key_default_compression"
        private const val KEY_BACKEND_URL = "key_backend_url"
    }
}
