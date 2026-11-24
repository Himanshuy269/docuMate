package com.example.documate.core.di

import android.content.Context
import com.example.documate.core.data.repository.AndroidDocumentRepository
import com.example.documate.core.data.repository.DefaultOfficeConversionRepository
import com.example.documate.core.data.repository.DefaultPdfRepository
import com.example.documate.core.data.repository.FileHistoryRepository
import com.example.documate.core.data.repository.SharedPreferencesSettingsRepository
import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.core.domain.repository.SettingsRepository
import com.example.documate.core.domain.usecase.ConvertOfficeToPdfUseCase
import com.example.documate.feature.add_images_to_pdf.domain.usecase.AddImagesToPdfUseCase
import com.example.documate.feature.excel_to_pdf.domain.usecase.ConvertExcelToPdfUseCase
import com.example.documate.feature.images_to_pdf.domain.usecase.ConvertImagesToPdfUseCase
import com.example.documate.feature.pdf_compressor.domain.usecase.CompressPdfUseCase
import com.example.documate.feature.powerpoint_to_pdf.domain.usecase.ConvertPowerPointToPdfUseCase
import com.example.documate.feature.word_to_pdf.domain.usecase.ConvertWordToPdfUseCase

/**
 * Dependency Injection container providing application-scoped singleton instances.
 * Follows Clean Architecture by decoupling UI from direct data sources.
 */
interface AppContainer {
    val historyRepository: HistoryRepository
    val settingsRepository: SettingsRepository
    val documentRepository: DocumentRepository
    val pdfRepository: PdfRepository
    val officeConversionRepository: OfficeConversionRepository
    val convertImagesToPdfUseCase: ConvertImagesToPdfUseCase
    val addImagesToPdfUseCase: AddImagesToPdfUseCase
    val compressPdfUseCase: CompressPdfUseCase
    val convertOfficeToPdfUseCase: ConvertOfficeToPdfUseCase
    val convertWordToPdfUseCase: ConvertWordToPdfUseCase
    val convertExcelToPdfUseCase: ConvertExcelToPdfUseCase
    val convertPowerPointToPdfUseCase: ConvertPowerPointToPdfUseCase
}

/**
 * Default implementation of AppContainer initialized with Application Context.
 */
class DefaultAppContainer(private val applicationContext: Context) : AppContainer {
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

    override val addImagesToPdfUseCase: AddImagesToPdfUseCase by lazy {
        AddImagesToPdfUseCase(
            documentRepository = documentRepository,
            pdfRepository = pdfRepository,
            historyRepository = historyRepository
        )
    }

    override val compressPdfUseCase: CompressPdfUseCase by lazy {
        CompressPdfUseCase(
            documentRepository = documentRepository,
            pdfRepository = pdfRepository,
            historyRepository = historyRepository
        )
    }

    override val convertOfficeToPdfUseCase: ConvertOfficeToPdfUseCase by lazy {
        ConvertOfficeToPdfUseCase(
            documentRepository = documentRepository,
            officeConversionRepository = officeConversionRepository,
            historyRepository = historyRepository,
            settingsRepository = settingsRepository
        )
    }

    override val convertWordToPdfUseCase: ConvertWordToPdfUseCase by lazy {
        ConvertWordToPdfUseCase(
            documentRepository = documentRepository,
            officeConversionRepository = officeConversionRepository,
            historyRepository = historyRepository
        )
    }

    override val convertExcelToPdfUseCase: ConvertExcelToPdfUseCase by lazy {
        ConvertExcelToPdfUseCase(
            documentRepository = documentRepository,
            officeConversionRepository = officeConversionRepository,
            historyRepository = historyRepository
        )
    }

    override val convertPowerPointToPdfUseCase: ConvertPowerPointToPdfUseCase by lazy {
        ConvertPowerPointToPdfUseCase(
            documentRepository = documentRepository,
            officeConversionRepository = officeConversionRepository,
            historyRepository = historyRepository
        )
    }
}
