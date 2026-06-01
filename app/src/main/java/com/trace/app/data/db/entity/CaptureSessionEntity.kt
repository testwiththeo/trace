package com.trace.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for capture sessions.
 */
@Entity(
    tableName = "capture_sessions",
    indices = [Index(value = ["started_at"])]
)
data class CaptureSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "ended_at")
    val endedAt: Long?,

    @ColumnInfo(name = "traffic_count")
    val trafficCount: Int,

    @ColumnInfo(name = "app_packages")
    val appPackages: String // Comma-separated
)
