package com.example.documate.core.domain.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing local conversion history.
 */
interface HistoryRepository {
    fun getRecentConversions(): Flow<List<ConversionRecord>>

    suspend fun addRecord(record: ConversionRecord): AppResult<Unit>

    suspend fun deleteRecord(id: String): AppResult<Unit>

    suspend fun clearAll(): AppResult<Unit>
}
