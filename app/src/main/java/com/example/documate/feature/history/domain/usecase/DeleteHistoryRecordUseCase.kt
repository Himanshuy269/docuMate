package com.example.documate.feature.history.domain.usecase

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.repository.HistoryRepository

/**
 * UseCase to remove an individual conversion record by its unique identifier.
 */
class DeleteHistoryRecordUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(recordId: String): AppResult<Unit> {
        val trimmed = recordId.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Error("Record ID cannot be empty")
        }
        return historyRepository.deleteRecord(trimmed)
    }
}
