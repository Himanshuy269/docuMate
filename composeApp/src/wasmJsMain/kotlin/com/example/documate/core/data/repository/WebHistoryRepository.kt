package com.example.documate.core.data.repository

import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WebHistoryRepository : HistoryRepository {

    private val _records = MutableStateFlow<List<ConversionRecord>>(emptyList())

    override fun getRecentConversions(): Flow<List<ConversionRecord>> = _records.asStateFlow()

    override suspend fun addRecord(record: ConversionRecord): AppResult<Unit> {
        _records.update { current ->
            listOf(record) + current.filter { it.id != record.id }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun deleteRecord(id: String): AppResult<Unit> {
        _records.update { current -> current.filter { it.id != id } }
        return AppResult.Success(Unit)
    }

    override suspend fun clearAll(): AppResult<Unit> {
        _records.value = emptyList()
        return AppResult.Success(Unit)
    }
}
