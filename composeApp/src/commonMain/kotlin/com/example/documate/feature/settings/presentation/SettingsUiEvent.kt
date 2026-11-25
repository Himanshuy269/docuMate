package com.example.documate.feature.settings.presentation

import com.example.documate.core.domain.model.AppThemeMode
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize

sealed interface SettingsUiEvent {
    data class SelectTheme(val theme: AppThemeMode) : SettingsUiEvent
    data class SelectDefaultPageSize(val size: PdfPageSize) : SettingsUiEvent
    data class SelectDefaultCompression(val preset: CompressionPreset) : SettingsUiEvent
    data class UpdateBackendUrl(val url: String) : SettingsUiEvent
    data object ShowClearCacheConfirmation : SettingsUiEvent
    data object DismissClearCacheConfirmation : SettingsUiEvent
    data object ClearAppCache : SettingsUiEvent
    data object DismissStatusMessage : SettingsUiEvent
}
