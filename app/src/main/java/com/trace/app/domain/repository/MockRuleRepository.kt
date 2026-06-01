package com.trace.app.domain.repository

import com.trace.app.domain.model.MockRule
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for mock rules.
 * Pure Kotlin - no Android dependencies.
 */
interface MockRuleRepository {
    fun getAllRules(): Flow<List<MockRule>>
    fun getEnabledRules(): Flow<List<MockRule>>
    suspend fun getRuleById(id: Long): MockRule?
    suspend fun insertRule(rule: MockRule): Long
    suspend fun updateRule(rule: MockRule)
    suspend fun deleteRule(rule: MockRule)
    suspend fun toggleRule(id: Long, enabled: Boolean)
}
