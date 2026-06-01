package com.trace.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trace.app.data.db.entity.CaptureSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for capture session operations.
 */
@Dao
interface SessionDao {

    @Query("SELECT * FROM capture_sessions ORDER BY started_at DESC")
    fun getAllSessions(): Flow<List<CaptureSessionEntity>>

    @Query("SELECT * FROM capture_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): CaptureSessionEntity?

    @Query("SELECT * FROM capture_sessions WHERE ended_at IS NULL LIMIT 1")
    suspend fun getActiveSession(): CaptureSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CaptureSessionEntity): Long

    @Update
    suspend fun updateSession(session: CaptureSessionEntity)

    @Query("UPDATE capture_sessions SET ended_at = :endedAt WHERE id = :id")
    suspend fun endSession(id: Long, endedAt: Long)
}
