package com.trace.app.data.repository

import com.trace.app.data.db.dao.MockRuleDao
import com.trace.app.data.repository.TraceMapper.toDomain
import com.trace.app.data.repository.TraceMapper.toEntity
import com.trace.app.domain.model.MockRule
import com.trace.app.domain.repository.MockRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MockRuleRepository using Room database.
 */
@Singleton
class MockRuleRepositoryImpl @Inject constructor(
    private val dao: MockRuleDao
) : MockRuleRepository {

    override fun getAllRules(): Flow<List<MockRule>> {
        return dao.getAllRules().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getEnabledRules(): Flow<List<MockRule>> {
        return dao.getEnabledRules().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getRuleById(id: Long): MockRule? {
        return dao.getRuleById(id)?.toDomain()
    }

    override suspend fun insertRule(rule: MockRule): Long {
        return dao.insertRule(rule.toEntity())
    }

    override suspend fun updateRule(rule: MockRule) {
        dao.updateRule(rule.toEntity())
    }

    override suspend fun deleteRule(rule: MockRule) {
        dao.deleteRule(rule.toEntity())
    }

    override suspend fun toggleRule(id: Long, enabled: Boolean) {
        dao.toggleRule(id, enabled)
    }
}
