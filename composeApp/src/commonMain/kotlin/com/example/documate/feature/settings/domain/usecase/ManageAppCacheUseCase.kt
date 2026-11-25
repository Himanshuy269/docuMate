package com.example.documate.feature.settings.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.repository.SettingsRepository

/**
 * UseCase to inspect and safely clear temporary files in the app cache and staging areas.
 */
class ManageAppCacheUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend fun getCacheSizeBytes(): Long {
        return settingsRepository.getAppCacheSizeBytes()
    }

    suspend fun clearCache(): AppResult<Long> {
        return settingsRepository.clearAppCache()
    }
}
