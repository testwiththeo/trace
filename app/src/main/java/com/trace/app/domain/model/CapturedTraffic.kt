package com.trace.app.domain.model

/**
 * Represents a captured HTTP/HTTPS request/response pair with metadata.
 */
data class CapturedTraffic(
    val id: Long = 0,
    val requestMethod: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val responseStatusCode: Int,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val durationMs: Long,
    val timestamp: Long,
    val appPackage: String,
    val isHttps: Boolean,
    val notes: String? = null
)
