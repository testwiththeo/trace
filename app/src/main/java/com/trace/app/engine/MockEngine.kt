package com.trace.app.engine

import com.trace.app.domain.model.MockRule
import com.trace.app.domain.repository.MockRuleRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates mock rules and returns custom responses when rules match.
 * Rules are evaluated in priority order (lower priority value = higher precedence).
 */
@Singleton
class MockEngine @Inject constructor(
    private val mockRuleRepository: MockRuleRepository
) {

    /**
     * Checks if a mock rule matches the request and returns the mock response.
     * Returns null if no rule matches.
     */
    suspend fun evaluateMockRules(
        url: String,
        method: String
    ): MockResponse? {
        return try {
            val enabledRules = mockRuleRepository.getEnabledRules().first()

            // Rules are already sorted by priority in the repository
            val matchingRule = enabledRules.firstOrNull { rule ->
                rule.matches(url, method)
            }

            if (matchingRule != null) {
                Timber.d("Mock rule matched: ${matchingRule.description ?: matchingRule.urlPattern}")

                MockResponse(
                    statusCode = matchingRule.responseStatusCode,
                    headers = matchingRule.responseHeaders ?: emptyMap(),
                    body = matchingRule.responseBody,
                    delayMs = matchingRule.delayMs
                )
            } else {
                null
            }

        } catch (e: Exception) {
            Timber.e(e, "Error evaluating mock rules")
            null
        }
    }

    /**
     * Checks if a URL should be mocked (without returning the full response).
     */
    suspend fun shouldMock(url: String, method: String): Boolean {
        return evaluateMockRules(url, method) != null
    }

    data class MockResponse(
        val statusCode: Int,
        val headers: Map<String, String>,
        val body: String?,
        val delayMs: Long
    )
}
