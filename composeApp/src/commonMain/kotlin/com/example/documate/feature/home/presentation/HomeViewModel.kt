package com.example.documate.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documate.core.domain.model.ToolCategory
import com.example.documate.core.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class HomeViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<ToolCategory?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery,
        _selectedCategory,
        historyRepository.getRecentConversions()
    ) { query, category, history ->
        HomeUiState(
            searchQuery = query,
            selectedCategory = category,
            recentConversions = history.take(3),
            isLoadingHistory = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoadingHistory = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSelectCategory(category: ToolCategory?) {
        _selectedCategory.update { current ->
            if (current == category) null else category
        }
    }
}
