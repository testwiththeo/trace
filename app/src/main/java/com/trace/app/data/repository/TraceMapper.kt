package com.trace.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trace.app.data.db.entity.CapturedTrafficEntity
import com.trace.app.data.db.entity.CaptureSessionEntity
import com.trace.app.data.db.entity.MockRuleEntity
import com.trace.app.domain.model.CapturedTraffic
import com.trace.app.domain.model.CaptureSession
import com.trace.app.domain.model.MockRule

/**
 * Mappers for converting between domain models and database entities.
 */
object TraceMapper {

    private val gson = Gson()

    // CapturedTraffic mappings
    fun CapturedTraffic.toEntity(): CapturedTrafficEntity = CapturedTrafficEntity(
        id = id,
        requestMethod = requestMethod,
        url = url,
        requestHeaders = gson.toJson(requestHeaders),
        requestBody = requestBody,
        responseStatusCode = responseStatusCode,
        responseHeaders = gson.toJson(responseHeaders),
        responseBody = responseBody,
        durationMs = durationMs,
        timestamp = timestamp,
        appPackage = appPackage,
        isHttps = isHttps,
        notes = notes
    )

    fun CapturedTrafficEntity.toDomain(): CapturedTraffic {
        val requestHeadersType = object : TypeToken<Map<String, String>>() {}.type
        val responseHeadersType = object : TypeToken<Map<String, String>>() {}.type

        return CapturedTraffic(
            id = id,
            requestMethod = requestMethod,
            url = url,
            requestHeaders = try {
                gson.fromJson(requestHeaders, requestHeadersType)
            } catch (e: Exception) {
                emptyMap()
            },
            requestBody = requestBody,
            responseStatusCode = responseStatusCode,
            responseHeaders = try {
                gson.fromJson(responseHeaders, responseHeadersType)
            } catch (e: Exception) {
                emptyMap()
            },
            responseBody = responseBody,
            durationMs = durationMs,
            timestamp = timestamp,
            appPackage = appPackage,
            isHttps = isHttps,
            notes = notes
        )
    }

    // MockRule mappings
    fun MockRule.toEntity(): MockRuleEntity = MockRuleEntity(
        id = id,
        urlPattern = urlPattern,
        method = method,
        responseStatusCode = responseStatusCode,
        responseHeaders = responseHeaders?.let { gson.toJson(it) },
        responseBody = responseBody,
        delayMs = delayMs,
        isEnabled = isEnabled,
        priority = priority,
        createdAt = createdAt,
        description = description
    )

    fun MockRuleEntity.toDomain(): MockRule {
        val headersType = object : TypeToken<Map<String, String>>() {}.type

        return MockRule(
            id = id,
            urlPattern = urlPattern,
            method = method,
            responseStatusCode = responseStatusCode,
            responseHeaders = responseHeaders?.let { headers ->
                try {
                    gson.fromJson<Map<String, String>>(headers, headersType)
                } catch (e: Exception) {
                    null
                }
            },
            responseBody = responseBody,
            delayMs = delayMs,
            isEnabled = isEnabled,
            priority = priority,
            createdAt = createdAt,
            description = description
        )
    }

    // CaptureSession mappings
    fun CaptureSession.toEntity(): CaptureSessionEntity = CaptureSessionEntity(
        id = id,
        name = name,
        startedAt = startedAt,
        endedAt = endedAt,
        trafficCount = trafficCount,
        appPackages = appPackages.joinToString(",")
    )

    fun CaptureSessionEntity.toDomain(): CaptureSession = CaptureSession(
        id = id,
        name = name,
        startedAt = startedAt,
        endedAt = endedAt,
        trafficCount = trafficCount,
        appPackages = appPackages.split(",").filter { it.isNotBlank() }
    )
}
