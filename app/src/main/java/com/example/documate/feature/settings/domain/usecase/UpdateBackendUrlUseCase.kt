package com.example.documate.feature.settings.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.repository.SettingsRepository

/**
 * UseCase to validate and update the LibreOffice backend conversion service endpoint.
 * Enforces valid URL protocol (http or https) and basic structure.
 */
class UpdateBackendUrlUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(rawUrl: String): AppResult<Unit> {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Error("Backend URL cannot be blank")
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return AppResult.Error("URL must start with http:// or https://")
        }
        return settingsRepository.updateBackendUrl(trimmed)
    }
}
