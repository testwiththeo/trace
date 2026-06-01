package com.trace.app.domain.repository

import com.trace.app.domain.model.CaptureSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for capture sessions.
 * Pure Kotlin - no Android dependencies.
 */
interface SessionRepository {
    fun getAllSessions(): Flow<List<CaptureSession>>
    suspend fun getSessionById(id: Long): CaptureSession?
    suspend fun getActiveSession(): CaptureSession?
    suspend fun insertSession(session: CaptureSession): Long
    suspend fun updateSession(session: CaptureSession)
    suspend fun endSession(id: Long)
}
