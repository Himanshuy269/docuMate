package com.example.documate.core.domain.model

import com.example.documate.feature.settings.presentation.AppThemeMode

/**
 * Domain model representing persisted user preferences and configuration.
 */
data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val defaultPageSize: PdfPageSize = PdfPageSize.A4,
    val defaultCompression: CompressionPreset = CompressionPreset.MEDIUM,
    val backendServiceUrl: String = "http://10.0.2.2:8080"
)
