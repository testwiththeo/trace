package com.trace.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trace.app.data.db.entity.MockRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for mock rule operations.
 */
@Dao
interface MockRuleDao {

    @Query("SELECT * FROM mock_rules ORDER BY priority ASC")
    fun getAllRules(): Flow<List<MockRuleEntity>>

    @Query("SELECT * FROM mock_rules WHERE is_enabled = 1 ORDER BY priority ASC")
    fun getEnabledRules(): Flow<List<MockRuleEntity>>

    @Query("SELECT * FROM mock_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): MockRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: MockRuleEntity): Long

    @Update
    suspend fun updateRule(rule: MockRuleEntity)

    @Delete
    suspend fun deleteRule(rule: MockRuleEntity)

    @Query("UPDATE mock_rules SET is_enabled = :enabled WHERE id = :id")
    suspend fun toggleRule(id: Long, enabled: Boolean)
}
