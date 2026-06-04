package com.trace.app.presentation.screen.trafficdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trace.app.domain.model.CapturedTraffic
import com.trace.app.domain.repository.TrafficRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrafficDetailViewModel @Inject constructor(
    private val trafficRepository: TrafficRepository
) : ViewModel() {

    private val _traffic = MutableStateFlow<CapturedTraffic?>(null)
    val traffic = _traffic.asStateFlow()

    fun loadTraffic(id: Long) {
        viewModelScope.launch {
            _traffic.value = trafficRepository.getTrafficById(id)
        }
    }
}
