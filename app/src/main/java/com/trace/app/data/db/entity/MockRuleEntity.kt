package com.trace.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for mock rules.
 */
@Entity(
    tableName = "mock_rules",
    indices = [
        Index(value = ["priority"]),
        Index(value = ["is_enabled"])
    ]
)
data class MockRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "url_pattern")
    val urlPattern: String,

    @ColumnInfo(name = "method")
    val method: String?,

    @ColumnInfo(name = "response_status_code")
    val responseStatusCode: Int,

    @ColumnInfo(name = "response_headers")
    val responseHeaders: String?, // JSON encoded

    @ColumnInfo(name = "response_body")
    val responseBody: String?,

    @ColumnInfo(name = "delay_ms")
    val delayMs: Long,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean,

    @ColumnInfo(name = "priority")
    val priority: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "description")
    val description: String?
)
