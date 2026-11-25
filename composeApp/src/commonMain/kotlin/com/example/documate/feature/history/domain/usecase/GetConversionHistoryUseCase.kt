package com.example.documate.feature.history.domain.usecase

import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.core.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UseCase to retrieve and optionally filter conversion records.
 * Provides a clean reactive flow of conversion history sorted by timestamp descending.
 */
class GetConversionHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(filterCategory: ToolCategory? = null): Flow<List<ConversionRecord>> {
        return historyRepository.getRecentConversions().map { records ->
            if (filterCategory == null) {
                records
            } else {
                records.filter { it.tool.category == filterCategory }
            }
        }
    }
}
