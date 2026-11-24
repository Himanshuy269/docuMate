package com.example.documate.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettingsAndCache()
    }

    private fun loadSettingsAndCache() {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettingsSync()
            val cacheBytes = settingsRepository.getAppCacheSizeBytes()
            _uiState.update {
                it.copy(
                    themeMode = currentSettings.themeMode,
                    defaultPageSize = currentSettings.defaultPageSize,
                    defaultCompression = currentSettings.defaultCompression,
                    backendServiceUrl = currentSettings.backendServiceUrl,
                    cacheSizeBytes = cacheBytes
                )
            }

            settingsRepository.getSettings().collect { userSettings ->
                _uiState.update {
                    it.copy(
                        themeMode = userSettings.themeMode,
                        defaultPageSize = userSettings.defaultPageSize,
                        defaultCompression = userSettings.defaultCompression,
                        backendServiceUrl = userSettings.backendServiceUrl
                    )
                }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SelectTheme -> {
                viewModelScope.launch {
                    settingsRepository.updateTheme(event.theme)
                }
            }
            is SettingsUiEvent.SelectDefaultPageSize -> {
                viewModelScope.launch {
                    settingsRepository.updateDefaultPageSize(event.size)
                }
            }
            is SettingsUiEvent.SelectDefaultCompression -> {
                viewModelScope.launch {
                    settingsRepository.updateDefaultCompression(event.preset)
                }
            }
            is SettingsUiEvent.UpdateBackendUrl -> {
                viewModelScope.launch {
                    settingsRepository.updateBackendUrl(event.url)
                }
            }
            is SettingsUiEvent.ShowClearCacheConfirmation -> {
                _uiState.update { it.copy(isConfirmClearCacheVisible = true) }
            }
            is SettingsUiEvent.DismissClearCacheConfirmation -> {
                _uiState.update { it.copy(isConfirmClearCacheVisible = false) }
            }
            is SettingsUiEvent.ClearAppCache -> {
                viewModelScope.launch {
                    when (val result = settingsRepository.clearAppCache()) {
                        is AppResult.Success -> {
                            val updatedSize = settingsRepository.getAppCacheSizeBytes()
                            _uiState.update {
                                it.copy(
                                    isConfirmClearCacheVisible = false,
                                    cacheSizeBytes = updatedSize,
                                    statusMessage = "Application cache successfully cleared"
                                )
                            }
                        }
                        is AppResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isConfirmClearCacheVisible = false,
                                    statusMessage = "Failed to clear cache: ${result.message}"
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
            is SettingsUiEvent.DismissStatusMessage -> {
                _uiState.update { it.copy(statusMessage = null) }
            }
        }
    }
}
