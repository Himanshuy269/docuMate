package com.example.documate.core.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.documate.DocuMateApplication
import com.example.documate.feature.add_images_to_pdf.presentation.AddImagesToPdfViewModel
import com.example.documate.feature.excel_to_pdf.presentation.ExcelToPdfViewModel
import com.example.documate.feature.history.presentation.HistoryViewModel
import com.example.documate.feature.home.presentation.HomeViewModel
import com.example.documate.feature.images_to_pdf.presentation.ImagesToPdfViewModel
import com.example.documate.feature.pdf_compressor.presentation.PdfCompressorViewModel
import com.example.documate.feature.powerpoint_to_pdf.presentation.PowerPointToPdfViewModel
import com.example.documate.feature.settings.presentation.SettingsViewModel
import com.example.documate.feature.word_to_pdf.presentation.WordToPdfViewModel

/**
 * Factory provider providing ViewModels with their required dependencies
 * from the application container.
 */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = docuMateApplication()
            HomeViewModel(app.container.historyRepository)
        }
        initializer {
            val app = docuMateApplication()
            HistoryViewModel(
                getConversionHistoryUseCase = app.container.getConversionHistoryUseCase,
                deleteHistoryRecordUseCase = app.container.deleteHistoryRecordUseCase,
                clearHistoryUseCase = app.container.clearHistoryUseCase
            )
        }
        initializer {
            val app = docuMateApplication()
            SettingsViewModel(
                getSettingsUseCase = app.container.getSettingsUseCase,
                updateThemeUseCase = app.container.updateThemeUseCase,
                updateDefaultPageSizeUseCase = app.container.updateDefaultPageSizeUseCase,
                updateDefaultCompressionUseCase = app.container.updateDefaultCompressionUseCase,
                updateBackendUrlUseCase = app.container.updateBackendUrlUseCase,
                manageAppCacheUseCase = app.container.manageAppCacheUseCase
            )
        }
        initializer {
            val app = docuMateApplication()
            ImagesToPdfViewModel(
                documentRepository = app.container.documentRepository,
                convertImagesToPdfUseCase = app.container.convertImagesToPdfUseCase
            )
        }
        initializer {
            val app = docuMateApplication()
            AddImagesToPdfViewModel(
                documentRepository = app.container.documentRepository,
                pdfRepository = app.container.pdfRepository,
                addImagesToPdfUseCase = app.container.addImagesToPdfUseCase
            )
        }
        initializer {
            val app = docuMateApplication()
            PdfCompressorViewModel(
                documentRepository = app.container.documentRepository,
                pdfRepository = app.container.pdfRepository,
                compressPdfUseCase = app.container.compressPdfUseCase
            )
        }
        initializer {
            val app = docuMateApplication()
            ExcelToPdfViewModel(
                documentRepository = app.container.documentRepository,
                convertExcelToPdfUseCase = app.container.convertExcelToPdfUseCase
            )
        }
        initializer {
            val app = docuMateApplication()
            WordToPdfViewModel(
                documentRepository = app.container.documentRepository,
                convertWordToPdfUseCase = app.container.convertWordToPdfUseCase
            )
        }
        initializer {
            val app = docuMateApplication()
            PowerPointToPdfViewModel(
                documentRepository = app.container.documentRepository,
                convertPowerPointToPdfUseCase = app.container.convertPowerPointToPdfUseCase
            )
        }
    }
}

fun CreationExtras.docuMateApplication(): DocuMateApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DocuMateApplication)
