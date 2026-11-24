package com.example.documate.core.di

import android.content.Context
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.history.data.InMemoryHistoryRepository

/**
 * Dependency Injection container providing application-scoped singleton instances.
 * Follows Clean Architecture by decoupling UI from direct data sources.
 */
interface AppContainer {
    val historyRepository: HistoryRepository
}

/**
 * Default implementation of AppContainer initialized with Application Context.
 */
class DefaultAppContainer(private val applicationContext: Context) : AppContainer {
    override val historyRepository: HistoryRepository by lazy {
        InMemoryHistoryRepository()
    }
}
