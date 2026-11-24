package com.example.documate.feature.settings.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SelectTheme -> _uiState.update { it.copy(themeMode = event.theme) }
            is SettingsUiEvent.SelectDefaultPageSize -> _uiState.update { it.copy(defaultPageSize = event.size) }
            is SettingsUiEvent.SelectDefaultCompression -> _uiState.update { it.copy(defaultCompression = event.preset) }
            is SettingsUiEvent.UpdateBackendUrl -> _uiState.update { it.copy(backendServiceUrl = event.url) }
        }
    }
}
