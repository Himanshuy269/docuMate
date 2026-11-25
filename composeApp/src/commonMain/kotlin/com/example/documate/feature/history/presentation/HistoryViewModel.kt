package com.example.documate.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.feature.history.domain.usecase.ClearHistoryUseCase
import com.example.documate.feature.history.domain.usecase.DeleteHistoryRecordUseCase
import com.example.documate.feature.history.domain.usecase.GetConversionHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val getConversionHistoryUseCase: GetConversionHistoryUseCase,
    private val deleteHistoryRecordUseCase: DeleteHistoryRecordUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getConversionHistoryUseCase().collect { records ->
                _uiState.update { it.copy(records = records) }
            }
        }
    }

    fun onEvent(event: HistoryUiEvent) {
        when (event) {
            is HistoryUiEvent.SelectFilter -> _uiState.update { it.copy(selectedFilter = event.category) }
            is HistoryUiEvent.SelectTab -> _uiState.update { it.copy(selectedTab = event.tab) }
            is HistoryUiEvent.UpdateSearch -> _uiState.update { it.copy(searchQuery = event.query) }
            is HistoryUiEvent.DeleteRecord -> handleDeleteRecord(event.id)
            is HistoryUiEvent.ShowClearConfirmation -> _uiState.update { it.copy(isClearHistoryDialogVisible = true) }
            is HistoryUiEvent.DismissClearConfirmation -> _uiState.update { it.copy(isClearHistoryDialogVisible = false) }
            is HistoryUiEvent.ConfirmClearAll -> handleClearAll()
            is HistoryUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun handleDeleteRecord(id: String) {
        viewModelScope.launch {
            when (val result = deleteHistoryRecordUseCase(id)) {
                is AppResult.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> {}
            }
        }
    }

    private fun handleClearAll() {
        viewModelScope.launch {
            when (val result = clearHistoryUseCase()) {
                is AppResult.Success -> _uiState.update { it.copy(isClearHistoryDialogVisible = false) }
                is AppResult.Error -> _uiState.update { it.copy(errorMessage = result.message, isClearHistoryDialogVisible = false) }
                else -> {}
            }
        }
    }
}
