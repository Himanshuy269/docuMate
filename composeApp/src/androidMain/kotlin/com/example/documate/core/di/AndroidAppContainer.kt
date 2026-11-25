package com.example.documate.core.di

import android.content.Context
import com.example.documate.core.data.repository.AndroidDocumentRepository
import com.example.documate.core.data.repository.AndroidOfficeConversionRepository
import com.example.documate.core.data.repository.AndroidPdfRepository
import com.example.documate.core.data.repository.FileHistoryRepository
import com.example.documate.core.data.repository.SharedPreferencesSettingsRepository
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.core.domain.repository.SettingsRepository

/**
 * Android implementation of AppContainer wiring native Android services,
 * ContentResolver, native PdfDocument engine, and SharedPreferences.
 */
class AndroidAppContainer(private val applicationContext: Context) : BaseAppContainer() {
    override val historyRepository: HistoryRepository by lazy {
        FileHistoryRepository(applicationContext)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SharedPreferencesSettingsRepository(applicationContext)
    }

    override val documentRepository: DocumentRepository by lazy {
        AndroidDocumentRepository(applicationContext)
    }

    override val pdfRepository: PdfRepository by lazy {
        AndroidPdfRepository(applicationContext)
    }

    override val officeConversionRepository: OfficeConversionRepository by lazy {
        AndroidOfficeConversionRepository(applicationContext)
    }
}
