package com.trace.app.data.repository

import com.trace.app.data.db.dao.SessionDao
import com.trace.app.data.repository.TraceMapper.toDomain
import com.trace.app.data.repository.TraceMapper.toEntity
import com.trace.app.domain.model.CaptureSession
import com.trace.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SessionRepository using Room database.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val dao: SessionDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<CaptureSession>> {
        return dao.getAllSessions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getSessionById(id: Long): CaptureSession? {
        return dao.getSessionById(id)?.toDomain()
    }

    override suspend fun getActiveSession(): CaptureSession? {
        return dao.getActiveSession()?.toDomain()
    }

    override suspend fun insertSession(session: CaptureSession): Long {
        return dao.insertSession(session.toEntity())
    }

    override suspend fun updateSession(session: CaptureSession) {
        dao.updateSession(session.toEntity())
    }

    override suspend fun endSession(id: Long) {
        dao.endSession(id, System.currentTimeMillis())
    }
}
