package com.example.documate.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.feature.history.domain.usecase.ClearHistoryUseCase
import com.example.documate.feature.history.domain.usecase.DeleteHistoryRecordUseCase
import com.example.documate.feature.history.domain.usecase.GetConversionHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val getConversionHistoryUseCase: GetConversionHistoryUseCase,
    private val deleteHistoryRecordUseCase: DeleteHistoryRecordUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<ToolCategory?>(null)
    private val _isConfirmDialogOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        getConversionHistoryUseCase(),
        _selectedFilter,
        _isConfirmDialogOpen,
        _errorMessage
    ) { records, filter, isDialogOpen, error ->
        HistoryUiState(
            records = records,
            selectedFilter = filter,
            isConfirmClearDialogOpen = isDialogOpen,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun onEvent(event: HistoryUiEvent) {
        when (event) {
            is HistoryUiEvent.SelectFilter -> _selectedFilter.value = event.category
            is HistoryUiEvent.DeleteRecord -> handleDeleteRecord(event.id)
            is HistoryUiEvent.ShowClearConfirmation -> _isConfirmDialogOpen.value = true
            is HistoryUiEvent.DismissClearConfirmation -> _isConfirmDialogOpen.value = false
            is HistoryUiEvent.ConfirmClearAll -> handleClearAll()
            is HistoryUiEvent.ClearError -> _errorMessage.value = null
        }
    }

    private fun handleDeleteRecord(id: String) {
        viewModelScope.launch {
            when (val result = deleteHistoryRecordUseCase(id)) {
                is AppResult.Error -> _errorMessage.value = result.message
                else -> {}
            }
        }
    }

    private fun handleClearAll() {
        viewModelScope.launch {
            when (val result = clearHistoryUseCase()) {
                is AppResult.Success -> _isConfirmDialogOpen.value = false
                is AppResult.Error -> {
                    _errorMessage.value = result.message
                    _isConfirmDialogOpen.value = false
                }
                else -> _isConfirmDialogOpen.value = false
            }
        }
    }
}
