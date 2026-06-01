package com.trace.app.domain.repository

import com.trace.app.domain.model.CapturedTraffic
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for captured traffic.
 * Pure Kotlin - no Android dependencies.
 */
interface TrafficRepository {
    fun getAllTraffic(): Flow<List<CapturedTraffic>>
    suspend fun getTrafficById(id: Long): CapturedTraffic?
    fun searchTraffic(query: String): Flow<List<CapturedTraffic>>
    suspend fun insertTraffic(traffic: CapturedTraffic): Long
    suspend fun deleteTraffic(traffic: CapturedTraffic)
    suspend fun deleteOldTraffic(beforeTimestamp: Long)
    suspend fun clearAll()
}
