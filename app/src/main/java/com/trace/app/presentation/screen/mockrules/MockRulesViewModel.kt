package com.trace.app.presentation.screen.mockrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trace.app.domain.model.MockRule
import com.trace.app.domain.repository.MockRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MockRulesViewModel @Inject constructor(
    private val mockRuleRepository: MockRuleRepository
) : ViewModel() {

    val rules: StateFlow<List<MockRule>> = mockRuleRepository.getAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRule(urlPattern: String, statusCode: Int, body: String) {
        viewModelScope.launch {
            val rule = MockRule(
                urlPattern = urlPattern,
                responseStatusCode = statusCode,
                responseBody = body,
                isEnabled = true
            )
            mockRuleRepository.insertRule(rule)
        }
    }

    fun toggleRule(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            mockRuleRepository.toggleRule(id, enabled)
        }
    }

    fun deleteRule(rule: MockRule) {
        viewModelScope.launch {
            mockRuleRepository.deleteRule(rule)
        }
    }
}
