package com.trace.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trace.app.data.db.entity.CapturedTrafficEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for captured traffic operations.
 */
@Dao
interface TrafficDao {

    @Query("SELECT * FROM captured_traffic ORDER BY timestamp DESC")
    fun getAllTraffic(): Flow<List<CapturedTrafficEntity>>

    @Query("SELECT * FROM captured_traffic WHERE id = :id")
    suspend fun getTrafficById(id: Long): CapturedTrafficEntity?

    @Query("SELECT * FROM captured_traffic WHERE url LIKE '%' || :query || '%' OR request_method LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTraffic(query: String): Flow<List<CapturedTrafficEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraffic(traffic: CapturedTrafficEntity): Long

    @Delete
    suspend fun deleteTraffic(traffic: CapturedTrafficEntity)

    @Query("DELETE FROM captured_traffic WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldTraffic(beforeTimestamp: Long)

    @Query("DELETE FROM captured_traffic")
    suspend fun clearAll()
}
