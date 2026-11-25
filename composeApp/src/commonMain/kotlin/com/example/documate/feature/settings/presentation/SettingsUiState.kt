package com.example.documate.feature.settings.presentation

import com.example.documate.core.common.formatFileSize
import com.example.documate.core.domain.model.AppThemeMode
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val defaultPageSize: PdfPageSize = PdfPageSize.A4,
    val defaultCompression: CompressionPreset = CompressionPreset.MEDIUM,
    val backendServiceUrl: String = "http://10.0.2.2:8080",
    val cacheSizeBytes: Long = 0L,
    val isSavedSuccessMessageVisible: Boolean = false,
    val isConfirmClearCacheVisible: Boolean = false,
    val statusMessage: String? = null
) {
    val formattedCacheSize: String
        get() = formatFileSize(cacheSizeBytes)
}
