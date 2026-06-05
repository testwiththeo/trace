package com.trace.app.engine

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TrafficCapturerTest {

    private lateinit var trafficCapturer: TrafficCapturer

    @BeforeEach
    fun setup() {
        trafficCapturer = TrafficCapturer()
    }

    @Test
    fun `captureTraffic creates CapturedTraffic with all fields`() {
        // When
        val result = trafficCapturer.captureTraffic(
            method = "GET",
            url = "https://example.com/api/users",
            requestHeaders = mapOf("Authorization" to "Bearer token"),
            requestBody = null,
            responseStatusCode = 200,
            responseHeaders = mapOf("Content-Type" to "application/json"),
            responseBody = """{"users": []}""",
            durationMs = 342,
            appPackage = "com.example.app",
            isHttps = true
        )

        // Then
        result.requestMethod shouldBe "GET"
        result.url shouldBe "https://example.com/api/users"
        result.requestHeaders["Authorization"] shouldBe "Bearer token"
        result.requestBody shouldBe null
        result.responseStatusCode shouldBe 200
        result.responseHeaders["Content-Type"] shouldBe "application/json"
        result.responseBody shouldBe """{"users": []}"""
        result.durationMs shouldBe 342
        result.appPackage shouldBe "com.example.app"
        result.isHttps shouldBe true
    }

    @Test
    fun `captureTraffic truncates large request body`() {
        // Given
        val largeBody = "x".repeat(1024 * 1024 + 100) // 1MB + 100 bytes

        // When
        val result = trafficCapturer.captureTraffic(
            method = "POST",
            url = "https://example.com/api/upload",
            requestHeaders = emptyMap(),
            requestBody = largeBody,
            responseStatusCode = 200,
            responseHeaders = emptyMap(),
            responseBody = null,
            durationMs = 1000,
            appPackage = "com.example.app",
            isHttps = true
        )

        // Then
        result.requestBody shouldNotBe null
        result.requestBody!!.contains("[TRUNCATED") shouldBe true
        // Truncated to 1MB + message (approx 40 chars)
        (result.requestBody!!.length > (1024 * 1024)) shouldBe true
    }

    @Test
    fun `captureTraffic truncates large response body`() {
        // Given
        val largeBody = "y".repeat(1024 * 1024 + 200)

        // When
        val result = trafficCapturer.captureTraffic(
            method = "GET",
            url = "https://example.com/api/data",
            requestHeaders = emptyMap(),
            requestBody = null,
            responseStatusCode = 200,
            responseHeaders = emptyMap(),
            responseBody = largeBody,
            durationMs = 500,
            appPackage = "com.example.app",
            isHttps = false
        )

        // Then
        result.responseBody shouldNotBe null
        result.responseBody!!.contains("[TRUNCATED") shouldBe true
    }

    @Test
    fun `parseHttpRequest parses valid HTTP request`() {
        // Given
        val rawRequest =
            "GET /api/users HTTP/1.1\r\nHost: example.com\r\nAuthorization: Bearer token123\r\nContent-Type: application/json\r\n\r\n{\"query\": \"test\"}"

        // When
        val result = trafficCapturer.parseHttpRequest(rawRequest)

        // Then
        result shouldNotBe null
        result!!.method shouldBe "GET"
        result.path shouldBe "/api/users"
        result.headers["Host"] shouldBe "example.com"
        result.headers["Authorization"] shouldBe "Bearer token123"
        result.body shouldBe """{"query": "test"}"""
    }

    @Test
    fun `parseHttpRequest returns null for invalid request`() {
        // Given
        val invalidRequest = "INVALID"

        // When
        val result = trafficCapturer.parseHttpRequest(invalidRequest)

        // Then
        result shouldBe null
    }

    @Test
    fun `parseHttpResponse parses valid HTTP response`() {
        // Given
        val rawResponse =
            "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 15\r\n\r\n{\"status\":\"ok\"}"

        // When
        val result = trafficCapturer.parseHttpResponse(rawResponse)

        // Then
        result shouldNotBe null
        result!!.statusCode shouldBe 200
        result.statusMessage shouldBe "OK"
        result.headers["Content-Type"] shouldBe "application/json"
        result.body shouldBe """{"status":"ok"}"""
    }

    @Test
    fun `parseHttpResponse returns null for invalid response`() {
        // Given
        val invalidResponse = "INVALID RESPONSE"

        // When
        val result = trafficCapturer.parseHttpResponse(invalidResponse)

        // Then
        result shouldBe null
    }

    @Test
    fun `parseHttpRequest handles request without body`() {
        // Given
        val rawRequest = "GET /api/users HTTP/1.1\r\nHost: example.com\r\n\r\n"

        // When
        val result = trafficCapturer.parseHttpRequest(rawRequest)

        // Then
        result shouldNotBe null
        result!!.method shouldBe "GET"
        result.body shouldBe ""
    }
}
