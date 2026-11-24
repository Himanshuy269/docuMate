package com.example.documate.core.data.repository

import android.content.Context
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.domain.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Robust JSON file-backed implementation of HistoryRepository.
 * Persists conversion records to app-private internal storage across app restarts.
 */
class FileHistoryRepository(
    private val context: Context,
    fileName: String = "conversion_history.json"
) : HistoryRepository {

    private val historyFile: File = File(context.filesDir, fileName)
    private val mutex = Mutex()
    private val _records = MutableStateFlow<List<ConversionRecord>>(emptyList())

    init {
        _records.value = loadRecordsFromDisk()
    }

    override fun getRecentConversions(): Flow<List<ConversionRecord>> = _records.asStateFlow()

    override suspend fun addRecord(record: ConversionRecord): AppResult<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                _records.update { current ->
                    listOf(record) + current.filter { it.id != record.id }
                }
                saveRecordsToDisk(_records.value)
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error("Failed to persist conversion record: ${e.message}", e)
            }
        }
    }

    override suspend fun deleteRecord(id: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                _records.update { current -> current.filter { it.id != id } }
                saveRecordsToDisk(_records.value)
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error("Failed to delete record: ${e.message}", e)
            }
        }
    }

    override suspend fun clearAll(): AppResult<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                _records.value = emptyList()
                if (historyFile.exists()) {
                    historyFile.delete()
                }
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error("Failed to clear conversion history: ${e.message}", e)
            }
        }
    }

    private fun loadRecordsFromDisk(): List<ConversionRecord> {
        if (!historyFile.exists()) return emptyList()
        return try {
            val content = historyFile.readText()
            if (content.isBlank()) return emptyList()
            val array = JSONArray(content)
            val list = mutableListOf<ConversionRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val toolId = obj.optString("toolId", "")
                val tool = DocumentTool.fromId(toolId) ?: DocumentTool.IMAGES_TO_PDF
                list.add(
                    ConversionRecord(
                        id = obj.getString("id"),
                        tool = tool,
                        sourceFileName = obj.optString("sourceFileName", "Unknown"),
                        outputFileName = obj.optString("outputFileName", "Output.pdf"),
                        outputPath = obj.optString("outputPath", ""),
                        originalSizeBytes = obj.optLong("originalSizeBytes", 0L),
                        outputSizeBytes = obj.optLong("outputSizeBytes", 0L),
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.timestampMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveRecordsToDisk(records: List<ConversionRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("toolId", record.tool.id)
                put("sourceFileName", record.sourceFileName)
                put("outputFileName", record.outputFileName)
                put("outputPath", record.outputPath)
                put("originalSizeBytes", record.originalSizeBytes)
                put("outputSizeBytes", record.outputSizeBytes)
                put("timestampMillis", record.timestampMillis)
            }
            array.put(obj)
        }
        val tempFile = File(context.filesDir, "${historyFile.name}.tmp")
        tempFile.writeText(array.toString(2))
        tempFile.renameTo(historyFile)
    }
}
