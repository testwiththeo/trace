package com.trace.app.domain.model

/**
 * Groups captured traffic by session for organization.
 */
data class CaptureSession(
    val id: Long = 0,
    val name: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val trafficCount: Int = 0,
    val appPackages: List<String> = emptyList()
) {
    val isActive: Boolean
        get() = endedAt == null

    val durationMs: Long
        get() = (endedAt ?: System.currentTimeMillis()) - startedAt
}
