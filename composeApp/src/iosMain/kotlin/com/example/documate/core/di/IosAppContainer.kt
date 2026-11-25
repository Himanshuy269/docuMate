package com.example.documate.core.di

import com.example.documate.core.data.repository.IosDocumentRepository
import com.example.documate.core.data.repository.IosHistoryRepository
import com.example.documate.core.data.repository.IosOfficeConversionRepository
import com.example.documate.core.data.repository.IosPdfRepository
import com.example.documate.core.data.repository.IosSettingsRepository
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.core.domain.repository.SettingsRepository

/**
 * iOS implementation of AppContainer wiring Apple Foundation, PDFKit, and NSUserDefaults.
 */
class IosAppContainer : BaseAppContainer() {
    override val historyRepository: HistoryRepository by lazy {
        IosHistoryRepository()
    }

    override val settingsRepository: SettingsRepository by lazy {
        IosSettingsRepository()
    }

    override val documentRepository: DocumentRepository by lazy {
        IosDocumentRepository()
    }

    override val pdfRepository: PdfRepository by lazy {
        IosPdfRepository()
    }

    override val officeConversionRepository: OfficeConversionRepository by lazy {
        IosOfficeConversionRepository()
    }
}
