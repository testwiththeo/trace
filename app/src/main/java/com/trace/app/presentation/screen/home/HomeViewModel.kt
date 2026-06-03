package com.trace.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trace.app.domain.repository.TrafficRepository
import com.trace.app.proxy.LocalProxyServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val trafficCount: Int = 0,
    val isCapturing: Boolean = false,
    val proxyPort: Int? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trafficRepository: TrafficRepository,
    private val proxyServer: LocalProxyServer
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        trafficRepository.getAllTraffic(),
        proxyServer.status
    ) { traffic, proxyStatus ->
        HomeUiState(
            trafficCount = traffic.size,
            isCapturing = proxyStatus is LocalProxyServer.ProxyStatus.Running,
            proxyPort = if (proxyStatus is LocalProxyServer.ProxyStatus.Running) proxyStatus.port else null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
