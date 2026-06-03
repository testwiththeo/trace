package com.trace.app.presentation.screen.trafficlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trace.app.domain.model.CapturedTraffic
import com.trace.app.domain.repository.TrafficRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrafficLogViewModel @Inject constructor(
    private val trafficRepository: TrafficRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    val trafficList: StateFlow<List<CapturedTraffic>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                trafficRepository.getAllTraffic()
            } else {
                trafficRepository.searchTraffic(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearAll() {
        viewModelScope.launch {
            trafficRepository.clearAll()
        }
    }
}
