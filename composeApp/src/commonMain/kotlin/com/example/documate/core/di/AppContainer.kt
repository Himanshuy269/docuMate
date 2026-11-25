package com.example.documate.core.di

import com.example.documate.core.domain.repository.DocumentRepository
import com.example.documate.core.domain.repository.HistoryRepository
import com.example.documate.core.domain.repository.OfficeConversionRepository
import com.example.documate.core.domain.repository.PdfRepository
import com.example.documate.core.domain.repository.SettingsRepository
import com.example.documate.core.domain.usecase.ConvertOfficeToPdfUseCase
import com.example.documate.feature.add_images_to_pdf.domain.usecase.AddImagesToPdfUseCase
import com.example.documate.feature.excel_to_pdf.domain.usecase.ConvertExcelToPdfUseCase
import com.example.documate.feature.history.domain.usecase.ClearHistoryUseCase
import com.example.documate.feature.history.domain.usecase.DeleteHistoryRecordUseCase
import com.example.documate.feature.history.domain.usecase.GetConversionHistoryUseCase
import com.example.documate.feature.images_to_pdf.domain.usecase.ConvertImagesToPdfUseCase
import com.example.documate.feature.pdf_compressor.domain.usecase.CompressPdfUseCase
import com.example.documate.feature.powerpoint_to_pdf.domain.usecase.ConvertPowerPointToPdfUseCase
import com.example.documate.feature.settings.domain.usecase.GetSettingsUseCase
import com.example.documate.feature.settings.domain.usecase.ManageAppCacheUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateBackendUrlUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateDefaultCompressionUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateDefaultPageSizeUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateThemeUseCase
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

    // History UseCases
    val getConversionHistoryUseCase: GetConversionHistoryUseCase
    val deleteHistoryRecordUseCase: DeleteHistoryRecordUseCase
    val clearHistoryUseCase: ClearHistoryUseCase

    // Settings UseCases
    val getSettingsUseCase: GetSettingsUseCase
    val updateThemeUseCase: UpdateThemeUseCase
    val updateDefaultPageSizeUseCase: UpdateDefaultPageSizeUseCase
    val updateDefaultCompressionUseCase: UpdateDefaultCompressionUseCase
    val updateBackendUrlUseCase: UpdateBackendUrlUseCase
    val manageAppCacheUseCase: ManageAppCacheUseCase
}

abstract class BaseAppContainer : AppContainer {
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

    override val getConversionHistoryUseCase: GetConversionHistoryUseCase by lazy {
        GetConversionHistoryUseCase(historyRepository)
    }

    override val deleteHistoryRecordUseCase: DeleteHistoryRecordUseCase by lazy {
        DeleteHistoryRecordUseCase(historyRepository)
    }

    override val clearHistoryUseCase: ClearHistoryUseCase by lazy {
        ClearHistoryUseCase(historyRepository)
    }

    override val getSettingsUseCase: GetSettingsUseCase by lazy {
        GetSettingsUseCase(settingsRepository)
    }

    override val updateThemeUseCase: UpdateThemeUseCase by lazy {
        UpdateThemeUseCase(settingsRepository)
    }

    override val updateDefaultPageSizeUseCase: UpdateDefaultPageSizeUseCase by lazy {
        UpdateDefaultPageSizeUseCase(settingsRepository)
    }

    override val updateDefaultCompressionUseCase: UpdateDefaultCompressionUseCase by lazy {
        UpdateDefaultCompressionUseCase(settingsRepository)
    }

    override val updateBackendUrlUseCase: UpdateBackendUrlUseCase by lazy {
        UpdateBackendUrlUseCase(settingsRepository)
    }

    override val manageAppCacheUseCase: ManageAppCacheUseCase by lazy {
        ManageAppCacheUseCase(settingsRepository)
    }
}

object AppContainerHolder {
    lateinit var current: AppContainer
    val isInitialized: Boolean
        get() = ::current.isInitialized
}
