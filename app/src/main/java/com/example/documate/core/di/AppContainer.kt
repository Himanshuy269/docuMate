package com.example.documate.core.di

import android.content.Context
import com.example.documate.core.data.repository.AndroidDocumentRepository
import com.example.documate.core.data.repository.DefaultOfficeConversionRepository
import com.example.documate.core.data.repository.DefaultPdfRepository
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.feature.history.data.InMemoryHistoryRepository
import com.example.documate.feature.images_to_pdf.domain.usecase.ConvertImagesToPdfUseCase

/**
 * Dependency Injection container providing application-scoped singleton instances.
 * Follows Clean Architecture by decoupling UI from direct data sources.
 */
interface AppContainer {
    val historyRepository: HistoryRepository
    val documentRepository: DocumentRepository
    val pdfRepository: PdfRepository
    val officeConversionRepository: OfficeConversionRepository
    val convertImagesToPdfUseCase: ConvertImagesToPdfUseCase
}

/**
 * Default implementation of AppContainer initialized with Application Context.
 */
class DefaultAppContainer(private val applicationContext: Context) : AppContainer {
    override val historyRepository: HistoryRepository by lazy {
        InMemoryHistoryRepository()
    }

    override val documentRepository: DocumentRepository by lazy {
        AndroidDocumentRepository(applicationContext)
    }

    override val pdfRepository: PdfRepository by lazy {
        DefaultPdfRepository(applicationContext)
    }

    override val officeConversionRepository: OfficeConversionRepository by lazy {
        DefaultOfficeConversionRepository(applicationContext)
    }

    override val convertImagesToPdfUseCase: ConvertImagesToPdfUseCase by lazy {
        ConvertImagesToPdfUseCase(
            documentRepository = documentRepository,
            pdfRepository = pdfRepository,
            historyRepository = historyRepository
        )
    }
}
