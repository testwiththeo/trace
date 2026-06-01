package com.trace.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for captured HTTP/HTTPS traffic.
 */
@Entity(
    tableName = "captured_traffic",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["app_package"]),
        Index(value = ["request_method"])
    ]
)
data class CapturedTrafficEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "request_method")
    val requestMethod: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "request_headers")
    val requestHeaders: String, // JSON encoded

    @ColumnInfo(name = "request_body")
    val requestBody: String?,

    @ColumnInfo(name = "response_status_code")
    val responseStatusCode: Int,

    @ColumnInfo(name = "response_headers")
    val responseHeaders: String, // JSON encoded

    @ColumnInfo(name = "response_body")
    val responseBody: String?,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "app_package")
    val appPackage: String,

    @ColumnInfo(name = "is_https")
    val isHttps: Boolean,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
