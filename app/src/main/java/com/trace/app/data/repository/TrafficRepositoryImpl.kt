package com.trace.app.data.repository

import com.trace.app.data.db.dao.TrafficDao
import com.trace.app.data.repository.TraceMapper.toDomain
import com.trace.app.data.repository.TraceMapper.toEntity
import com.trace.app.domain.model.CapturedTraffic
import com.trace.app.domain.repository.TrafficRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TrafficRepository using Room database.
 */
@Singleton
class TrafficRepositoryImpl @Inject constructor(
    private val dao: TrafficDao
) : TrafficRepository {

    override fun getAllTraffic(): Flow<List<CapturedTraffic>> {
        return dao.getAllTraffic().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getTrafficById(id: Long): CapturedTraffic? {
        return dao.getTrafficById(id)?.toDomain()
    }

    override fun searchTraffic(query: String): Flow<List<CapturedTraffic>> {
        return dao.searchTraffic(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertTraffic(traffic: CapturedTraffic): Long {
        return dao.insertTraffic(traffic.toEntity())
    }

    override suspend fun deleteTraffic(traffic: CapturedTraffic) {
        dao.deleteTraffic(traffic.toEntity())
    }

    override suspend fun deleteOldTraffic(beforeTimestamp: Long) {
        dao.deleteOldTraffic(beforeTimestamp)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
