package com.trace.app.engine

import com.trace.app.domain.model.MockRule
import com.trace.app.domain.repository.MockRuleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injects artificial latency for requests matching URL patterns.
 * Used for testing loading states and timeout behavior.
 */
@Singleton
class DelayInjector @Inject constructor(
    private val mockRuleRepository: MockRuleRepository
) {

    companion object {
        private const val MIN_DELAY_MS = 0L
        private const val MAX_DELAY_MS = 10000L // 10 seconds max
    }

    /**
     * Applies delay before response if matching rule has delay configured.
     * Returns the delay applied in milliseconds.
     */
    suspend fun applyDelay(url: String, method: String): Long {
        val delayMs = getDelayMs(url, method)

        if (delayMs > 0) {
            Timber.d("Applying ${delayMs}ms delay for $url")
            delay(delayMs)
        }

        return delayMs
    }

    /**
     * Gets the configured delay for a URL without applying it.
     */
    suspend fun getDelayMs(url: String, method: String): Long {
        return try {
            val enabledRules = mockRuleRepository.getEnabledRules().first()

            // Find first matching rule with delay configured
            val matchingRule = enabledRules.firstOrNull { rule ->
                rule.matches(url, method) && rule.delayMs > 0
            }

            matchingRule?.let { rule ->
                // Clamp delay to valid range
                rule.delayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
            } ?: 0L

        } catch (e: Exception) {
            Timber.e(e, "Error getting delay for $url")
            0L
        }
    }

    /**
     * Checks if any delay would be applied for the given URL.
     */
    suspend fun hasDelay(url: String, method: String): Boolean {
        return getDelayMs(url, method) > 0
    }
}
