package com.trace.app.engine

import com.trace.app.domain.model.MockRule
import com.trace.app.domain.repository.MockRuleRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DelayInjectorTest {

    private lateinit var mockRuleRepository: MockRuleRepository
    private lateinit var delayInjector: DelayInjector

    @BeforeEach
    fun setup() {
        mockRuleRepository = mockk()
        delayInjector = DelayInjector(mockRuleRepository)
    }

    @Test
    fun `getDelayMs returns zero when no rules match`() = runTest {
        // Given
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(emptyList())

        // When
        val result = delayInjector.getDelayMs("https://example.com/api/users", "GET")

        // Then
        result shouldBe 0L
    }

    @Test
    fun `getDelayMs returns delay from matching rule`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "*/api/slow",
            method = null,
            responseStatusCode = 200,
            delayMs = 2000,
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When
        val result = delayInjector.getDelayMs("https://example.com/api/slow", "GET")

        // Then
        result shouldBe 2000L
    }

    @Test
    fun `getDelayMs returns zero when rule has no delay`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "*/api/fast",
            method = null,
            responseStatusCode = 200,
            delayMs = 0,
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When
        val result = delayInjector.getDelayMs("https://example.com/api/fast", "GET")

        // Then
        result shouldBe 0L
    }

    @Test
    fun `getDelayMs respects method filter`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "*/api/slow",
            method = "POST",
            responseStatusCode = 200,
            delayMs = 3000,
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When - GET should not match
        val resultGet = delayInjector.getDelayMs("https://example.com/api/slow", "GET")

        // Then
        resultGet shouldBe 0L

        // When - POST should match
        val resultPost = delayInjector.getDelayMs("https://example.com/api/slow", "POST")

        // Then
        resultPost shouldBe 3000L
    }

    @Test
    fun `getDelayMs clamps delay to max value`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "*/api/slow",
            method = null,
            responseStatusCode = 200,
            delayMs = 15000, // Exceeds max of 10000
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When
        val result = delayInjector.getDelayMs("https://example.com/api/slow", "GET")

        // Then
        result shouldBe 10000L // Clamped to max
    }

    @Test
    fun `hasDelay returns true when delay configured`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "*/api/slow",
            method = null,
            responseStatusCode = 200,
            delayMs = 2000,
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When
        val result = delayInjector.hasDelay("https://example.com/api/slow", "GET")

        // Then
        result shouldBe true
    }

    @Test
    fun `hasDelay returns false when no delay configured`() = runTest {
        // Given
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(emptyList())

        // When
        val result = delayInjector.hasDelay("https://example.com/api/users", "GET")

        // Then
        result shouldBe false
    }

    @Test
    fun `getDelayMs returns highest priority rule delay`() = runTest {
        // Given
        val rule1 = MockRule(
            id = 1,
            urlPattern = "*/api/*",
            method = null,
            responseStatusCode = 200,
            delayMs = 1000,
            priority = 200,
            isEnabled = true
        )
        val rule2 = MockRule(
            id = 2,
            urlPattern = "*/api/slow",
            method = null,
            responseStatusCode = 200,
            delayMs = 5000,
            priority = 100, // Higher priority
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule2, rule1))

        // When
        val result = delayInjector.getDelayMs("https://example.com/api/slow", "GET")

        // Then
        result shouldBe 5000L
    }
}
