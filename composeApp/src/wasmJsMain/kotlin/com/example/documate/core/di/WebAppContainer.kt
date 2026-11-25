package com.example.documate.core.di

import com.example.documate.core.data.repository.WebDocumentRepository
import com.example.documate.core.data.repository.WebHistoryRepository
import com.example.documate.core.data.repository.WebOfficeConversionRepository
import com.example.documate.core.data.repository.WebPdfRepository
import com.example.documate.core.data.repository.WebSettingsRepository
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.core.domain.repository.SettingsRepository

/**
 * Web implementation of AppContainer wiring browser APIs, HTML5 File API, and localStorage.
 */
class WebAppContainer : BaseAppContainer() {
    override val historyRepository: HistoryRepository by lazy {
        WebHistoryRepository()
    }

    override val settingsRepository: SettingsRepository by lazy {
        WebSettingsRepository()
    }

    override val documentRepository: DocumentRepository by lazy {
        WebDocumentRepository()
    }

    override val pdfRepository: PdfRepository by lazy {
        WebPdfRepository()
    }

    override val officeConversionRepository: OfficeConversionRepository by lazy {
        WebOfficeConversionRepository()
    }
}
