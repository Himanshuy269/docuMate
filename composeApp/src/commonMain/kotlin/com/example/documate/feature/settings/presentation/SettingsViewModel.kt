package com.example.documate.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.feature.settings.domain.usecase.GetSettingsUseCase
import com.example.documate.feature.settings.domain.usecase.ManageAppCacheUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateBackendUrlUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateDefaultCompressionUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateDefaultPageSizeUseCase
import com.example.documate.feature.settings.domain.usecase.UpdateThemeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase,
    private val updateDefaultPageSizeUseCase: UpdateDefaultPageSizeUseCase,
    private val updateDefaultCompressionUseCase: UpdateDefaultCompressionUseCase,
    private val updateBackendUrlUseCase: UpdateBackendUrlUseCase,
    private val manageAppCacheUseCase: ManageAppCacheUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettingsAndCache()
    }

    private fun loadSettingsAndCache() {
        viewModelScope.launch {
            val currentSettings = getSettingsUseCase.getSync()
            val cacheBytes = manageAppCacheUseCase.getCacheSizeBytes()
            _uiState.update {
                it.copy(
                    themeMode = currentSettings.themeMode,
                    defaultPageSize = currentSettings.defaultPageSize,
                    defaultCompression = currentSettings.defaultCompression,
                    backendServiceUrl = currentSettings.backendServiceUrl,
                    cacheSizeBytes = cacheBytes
                )
            }

            getSettingsUseCase().collect { userSettings ->
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
                    when (val result = updateThemeUseCase(event.theme)) {
                        is AppResult.Error -> _uiState.update { it.copy(statusMessage = result.message) }
                        else -> {}
                    }
                }
            }
            is SettingsUiEvent.SelectDefaultPageSize -> {
                viewModelScope.launch {
                    when (val result = updateDefaultPageSizeUseCase(event.size)) {
                        is AppResult.Error -> _uiState.update { it.copy(statusMessage = result.message) }
                        else -> {}
                    }
                }
            }
            is SettingsUiEvent.SelectDefaultCompression -> {
                viewModelScope.launch {
                    when (val result = updateDefaultCompressionUseCase(event.preset)) {
                        is AppResult.Error -> _uiState.update { it.copy(statusMessage = result.message) }
                        else -> {}
                    }
                }
            }
            is SettingsUiEvent.UpdateBackendUrl -> {
                viewModelScope.launch {
                    when (val result = updateBackendUrlUseCase(event.url)) {
                        is AppResult.Error -> _uiState.update { it.copy(statusMessage = result.message) }
                        else -> {}
                    }
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
                    when (val result = manageAppCacheUseCase.clearCache()) {
                        is AppResult.Success -> {
                            val updatedSize = manageAppCacheUseCase.getCacheSizeBytes()
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
