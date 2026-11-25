package com.example.documate.feature.history.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.repository.HistoryRepository

/**
 * UseCase to clear all recorded conversions from storage.
 */
class ClearHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return historyRepository.clearAll()
    }
}
