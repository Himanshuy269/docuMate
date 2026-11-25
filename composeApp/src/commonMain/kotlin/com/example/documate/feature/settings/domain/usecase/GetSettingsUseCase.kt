package com.example.documate.feature.settings.domain.usecase

import com.example.documate.core.domain.model.UserSettings
import com.example.documate.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase to retrieve user preferences and application settings as a reactive flow.
 */
class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<UserSettings> = settingsRepository.getSettings()

    fun getSync(): UserSettings = settingsRepository.getSettingsSync()
}
