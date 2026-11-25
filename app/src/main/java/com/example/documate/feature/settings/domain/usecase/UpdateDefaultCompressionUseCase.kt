package com.example.documate.feature.settings.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.repository.SettingsRepository

/**
 * UseCase to update the default compression preset used for PDF processing.
 */
class UpdateDefaultCompressionUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(preset: CompressionPreset): AppResult<Unit> {
        return settingsRepository.updateDefaultCompression(preset)
    }
}
