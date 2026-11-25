package com.example.documate.core.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.documate.core.di.AppContainerHolder
import com.example.documate.feature.add_images_to_pdf.presentation.AddImagesToPdfViewModel
import com.example.documate.feature.excel_to_pdf.presentation.ExcelToPdfViewModel
import com.example.documate.feature.history.presentation.HistoryViewModel
import com.example.documate.feature.home.presentation.HomeViewModel
import com.example.documate.feature.images_to_pdf.presentation.ImagesToPdfViewModel
import com.example.documate.feature.pdf_compressor.presentation.PdfCompressorViewModel
import com.example.documate.feature.pdf_to_excel.presentation.PdfToExcelViewModel
import com.example.documate.feature.pdf_to_powerpoint.presentation.PdfToPowerPointViewModel
import com.example.documate.feature.pdf_to_word.presentation.PdfToWordViewModel
import com.example.documate.feature.powerpoint_to_pdf.presentation.PowerPointToPdfViewModel
import com.example.documate.feature.settings.presentation.SettingsViewModel
import com.example.documate.feature.word_to_pdf.presentation.WordToPdfViewModel

/**
 * Multiplatform factory providing ViewModels with application container dependencies.
 */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = AppContainerHolder.current
            HomeViewModel(container.historyRepository)
        }
        initializer {
            val container = AppContainerHolder.current
            HistoryViewModel(
                getConversionHistoryUseCase = container.getConversionHistoryUseCase,
                deleteHistoryRecordUseCase = container.deleteHistoryRecordUseCase,
                clearHistoryUseCase = container.clearHistoryUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            SettingsViewModel(
                getSettingsUseCase = container.getSettingsUseCase,
                updateThemeUseCase = container.updateThemeUseCase,
                updateDefaultPageSizeUseCase = container.updateDefaultPageSizeUseCase,
                updateDefaultCompressionUseCase = container.updateDefaultCompressionUseCase,
                updateBackendUrlUseCase = container.updateBackendUrlUseCase,
                manageAppCacheUseCase = container.manageAppCacheUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            ImagesToPdfViewModel(
                documentRepository = container.documentRepository,
                convertImagesToPdfUseCase = container.convertImagesToPdfUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            AddImagesToPdfViewModel(
                documentRepository = container.documentRepository,
                pdfRepository = container.pdfRepository,
                addImagesToPdfUseCase = container.addImagesToPdfUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            PdfCompressorViewModel(
                documentRepository = container.documentRepository,
                pdfRepository = container.pdfRepository,
                compressPdfUseCase = container.compressPdfUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            ExcelToPdfViewModel(
                documentRepository = container.documentRepository,
                convertExcelToPdfUseCase = container.convertExcelToPdfUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            WordToPdfViewModel(
                documentRepository = container.documentRepository,
                convertWordToPdfUseCase = container.convertWordToPdfUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            PowerPointToPdfViewModel(
                documentRepository = container.documentRepository,
                convertPowerPointToPdfUseCase = container.convertPowerPointToPdfUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            PdfToWordViewModel(
                documentRepository = container.documentRepository,
                convertPdfToWordUseCase = container.convertPdfToWordUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            PdfToExcelViewModel(
                documentRepository = container.documentRepository,
                convertPdfToExcelUseCase = container.convertPdfToExcelUseCase
            )
        }
        initializer {
            val container = AppContainerHolder.current
            PdfToPowerPointViewModel(
                documentRepository = container.documentRepository,
                convertPdfToPowerPointUseCase = container.convertPdfToPowerPointUseCase
            )
        }
    }
}
