package com.example.documate.feature.settings.presentation

import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize

enum class AppThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light Theme"),
    DARK("Dark Theme")
}

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
        get() = when {
            cacheSizeBytes <= 0 -> "0 KB"
            cacheSizeBytes < 1024 * 1024 -> String.format("%.1f KB", cacheSizeBytes / 1024.0)
            else -> String.format("%.2f MB", cacheSizeBytes / (1024.0 * 1024.0))
        }
}
