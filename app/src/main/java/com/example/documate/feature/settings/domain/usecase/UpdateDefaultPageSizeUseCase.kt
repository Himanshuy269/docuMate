package com.example.documate.feature.settings.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.repository.SettingsRepository

/**
 * UseCase to update the default PDF page dimensions used across document tools.
 */
class UpdateDefaultPageSizeUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(pageSize: PdfPageSize): AppResult<Unit> {
        return settingsRepository.updateDefaultPageSize(pageSize)
    }
}
