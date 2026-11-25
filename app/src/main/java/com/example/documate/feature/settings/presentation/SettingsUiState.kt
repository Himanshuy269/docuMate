package com.example.documate.feature.settings.presentation

import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize
import java.util.Locale

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
            cacheSizeBytes <= 0L -> "0 B"
            cacheSizeBytes < 1024L -> "$cacheSizeBytes B"
            cacheSizeBytes < 1024L * 1024L -> {
                val kb = cacheSizeBytes / 1024.0
                if (kb % 1.0 == 0.0) "${kb.toLong()} KB" else String.format(Locale.US, "%.1f KB", kb)
            }
            else -> {
                val mb = cacheSizeBytes / (1024.0 * 1024.0)
                if (mb % 1.0 == 0.0) "${mb.toLong()} MB" else String.format(Locale.US, "%.1f MB", mb)
            }
        }
}
