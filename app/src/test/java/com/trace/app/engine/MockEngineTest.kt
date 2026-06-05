package com.trace.app.engine

import com.trace.app.domain.model.MockRule
import com.trace.app.domain.repository.MockRuleRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MockEngineTest {

    private lateinit var mockRuleRepository: MockRuleRepository
    private lateinit var mockEngine: MockEngine

    @BeforeEach
    fun setup() {
        mockRuleRepository = mockk()
        mockEngine = MockEngine(mockRuleRepository)
    }

    @Test
    fun `evaluateMockRules returns null when no rules match`() = runTest {
        // Given
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(emptyList())

        // When
        val result = mockEngine.evaluateMockRules("https://example.com/api/users", "GET")

        // Then
        result shouldBe null
    }

    @Test
    fun `evaluateMockRules returns mock response when exact match found`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "https://example.com/api/users",
            method = "GET",
            responseStatusCode = 500,
            responseHeaders = mapOf("Content-Type" to "application/json"),
            responseBody = """{"error": "Server error"}""",
            delayMs = 0,
            isEnabled = true,
            priority = 100
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When
        val result = mockEngine.evaluateMockRules("https://example.com/api/users", "GET")

        // Then
        result shouldNotBe null
        result!!.statusCode shouldBe 500
        result.body shouldBe """{"error": "Server error"}"""
        result.headers["Content-Type"] shouldBe "application/json"
    }

    @Test
    fun `evaluateMockRules returns mock response for wildcard pattern`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "*/api/users/*",
            method = null, // Match all methods
            responseStatusCode = 200,
            responseHeaders = null,
            responseBody = """{"mocked": true}""",
            delayMs = 0,
            isEnabled = true,
            priority = 100
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When
        val result = mockEngine.evaluateMockRules("https://example.com/api/users/123", "GET")

        // Then
        result shouldNotBe null
        result!!.statusCode shouldBe 200
        result.body shouldBe """{"mocked": true}"""
    }

    @Test
    fun `evaluateMockRules returns highest priority rule when multiple match`() = runTest {
        // Given
        val rule1 = MockRule(
            id = 1,
            urlPattern = "*/api/*",
            method = null,
            responseStatusCode = 200,
            responseBody = "Low priority",
            priority = 200, // Lower priority
            isEnabled = true
        )
        val rule2 = MockRule(
            id = 2,
            urlPattern = "*/api/users*",
            method = null,
            responseStatusCode = 500,
            responseBody = "High priority",
            priority = 100, // Higher priority (lower value)
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule2, rule1))

        // When
        val result = mockEngine.evaluateMockRules("https://example.com/api/users", "GET")

        // Then
        result shouldNotBe null
        result!!.statusCode shouldBe 500
        result.body shouldBe "High priority"
    }

    @Test
    fun `evaluateMockRules respects method filter`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "*/api/users",
            method = "POST", // Only POST
            responseStatusCode = 201,
            responseBody = "Created",
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When - GET request should not match
        val resultGet = mockEngine.evaluateMockRules("https://example.com/api/users", "GET")

        // Then
        resultGet shouldBe null

        // When - POST request should match
        val resultPost = mockEngine.evaluateMockRules("https://example.com/api/users", "POST")

        // Then
        resultPost shouldNotBe null
        resultPost!!.statusCode shouldBe 201
    }

    @Test
    fun `evaluateMockRules includes delay from rule`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "*/api/slow",
            method = null,
            responseStatusCode = 200,
            responseBody = "Slow response",
            delayMs = 3000,
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When
        val result = mockEngine.evaluateMockRules("https://example.com/api/slow", "GET")

        // Then
        result shouldNotBe null
        result!!.delayMs shouldBe 3000
    }

    @Test
    fun `shouldMock returns true when rule matches`() = runTest {
        // Given
        val rule = MockRule(
            id = 1,
            urlPattern = "*/api/users",
            method = null,
            responseStatusCode = 200,
            isEnabled = true
        )
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(listOf(rule))

        // When
        val result = mockEngine.shouldMock("https://example.com/api/users", "GET")

        // Then
        result shouldBe true
    }

    @Test
    fun `shouldMock returns false when no rule matches`() = runTest {
        // Given
        coEvery { mockRuleRepository.getEnabledRules() } returns flowOf(emptyList())

        // When
        val result = mockEngine.shouldMock("https://example.com/api/users", "GET")

        // Then
        result shouldBe false
    }
}
