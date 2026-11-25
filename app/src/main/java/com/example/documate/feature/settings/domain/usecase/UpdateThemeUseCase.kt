package com.example.documate.feature.settings.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.repository.SettingsRepository
import com.example.documate.feature.settings.presentation.AppThemeMode

/**
 * UseCase to update the application visual theme preference.
 */
class UpdateThemeUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(themeMode: AppThemeMode): AppResult<Unit> {
        return settingsRepository.updateTheme(themeMode)
    }
}
