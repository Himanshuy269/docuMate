package com.example.documate.core.domain.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.model.UserSettings
import com.example.documate.core.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing application preferences and cache diagnostics.
 */
interface SettingsRepository {
    fun getSettings(): Flow<UserSettings>

    fun getSettingsSync(): UserSettings

    suspend fun updateTheme(themeMode: AppThemeMode): AppResult<Unit>

    suspend fun updateDefaultPageSize(pageSize: PdfPageSize): AppResult<Unit>

    suspend fun updateDefaultCompression(preset: CompressionPreset): AppResult<Unit>

    suspend fun updateBackendUrl(url: String): AppResult<Unit>

    suspend fun getAppCacheSizeBytes(): Long

    suspend fun clearAppCache(): AppResult<Long>
}
