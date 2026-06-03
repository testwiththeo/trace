package com.trace.app.engine

import com.trace.app.domain.model.CapturedTraffic
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses HTTP request/response and creates CapturedTraffic domain models.
 * Handles body truncation at 1MB.
 */
@Singleton
class TrafficCapturer @Inject constructor() {

    companion object {
        private const val MAX_BODY_SIZE = 1024 * 1024 // 1MB
    }

    /**
     * Creates a CapturedTraffic from raw HTTP data.
     */
    fun captureTraffic(
        method: String,
        url: String,
        requestHeaders: Map<String, String>,
        requestBody: String?,
        responseStatusCode: Int,
        responseHeaders: Map<String, String>,
        responseBody: String?,
        durationMs: Long,
        appPackage: String,
        isHttps: Boolean
    ): CapturedTraffic {
        return CapturedTraffic(
            requestMethod = method,
            url = url,
            requestHeaders = requestHeaders,
            requestBody = truncateBody(requestBody),
            responseStatusCode = responseStatusCode,
            responseHeaders = responseHeaders,
            responseBody = truncateBody(responseBody),
            durationMs = durationMs,
            timestamp = System.currentTimeMillis(),
            appPackage = appPackage,
            isHttps = isHttps
        )
    }

    /**
     * Truncates body if it exceeds MAX_BODY_SIZE.
     */
    private fun truncateBody(body: String?): String? {
        if (body == null) return null

        return if (body.length > MAX_BODY_SIZE) {
            val truncated = body.substring(0, MAX_BODY_SIZE)
            Timber.d("Body truncated from ${body.length} to $MAX_BODY_SIZE bytes")
            "$truncated\n\n[TRUNCATED - Original size: ${body.length} bytes]"
        } else {
            body
        }
    }

    /**
     * Parses raw HTTP request string into components.
     */
    fun parseHttpRequest(raw: String): HttpRequest? {
        return try {
            val lines = raw.split("\r\n")
            if (lines.isEmpty()) return null

            // Parse request line: GET /path HTTP/1.1
            val requestLine = lines[0].split(" ")
            if (requestLine.size < 3) return null

            val method = requestLine[0]
            val path = requestLine[1]

            // Parse headers
            val headers = mutableMapOf<String, String>()
            var bodyStartIndex = 0

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty()) {
                    bodyStartIndex = i + 1
                    break
                }

                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    val key = line.substring(0, colonIndex).trim()
                    val value = line.substring(colonIndex + 1).trim()
                    headers[key] = value
                }
            }

            // Extract body
            val body = if (bodyStartIndex < lines.size) {
                lines.subList(bodyStartIndex, lines.size).joinToString("\r\n")
            } else null

            HttpRequest(
                method = method,
                path = path,
                headers = headers,
                body = body
            )

        } catch (e: Exception) {
            Timber.e(e, "Error parsing HTTP request")
            null
        }
    }

    /**
     * Parses raw HTTP response string into components.
     */
    fun parseHttpResponse(raw: String): HttpResponse? {
        return try {
            val lines = raw.split("\r\n")
            if (lines.isEmpty()) return null

            // Parse status line: HTTP/1.1 200 OK
            val statusLine = lines[0].split(" ", limit = 3)
            if (statusLine.size < 2) return null

            val statusCode = statusLine[1].toIntOrNull() ?: return null
            val statusMessage = if (statusLine.size > 2) statusLine[2] else ""

            // Parse headers
            val headers = mutableMapOf<String, String>()
            var bodyStartIndex = 0

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty()) {
                    bodyStartIndex = i + 1
                    break
                }

                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    val key = line.substring(0, colonIndex).trim()
                    val value = line.substring(colonIndex + 1).trim()
                    headers[key] = value
                }
            }

            // Extract body
            val body = if (bodyStartIndex < lines.size) {
                lines.subList(bodyStartIndex, lines.size).joinToString("\r\n")
            } else null

            HttpResponse(
                statusCode = statusCode,
                statusMessage = statusMessage,
                headers = headers,
                body = body
            )

        } catch (e: Exception) {
            Timber.e(e, "Error parsing HTTP response")
            null
        }
    }

    data class HttpRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: String?
    )

    data class HttpResponse(
        val statusCode: Int,
        val statusMessage: String,
        val headers: Map<String, String>,
        val body: String?
    )
}
