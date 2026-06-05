package com.trace.app.engine

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BlocklistFilterTest {

    private lateinit var blocklistFilter: BlocklistFilter

    @BeforeEach
    fun setup() {
        blocklistFilter = BlocklistFilter()
    }

    @Test
    fun `isBlocked returns false for empty blocklist`() {
        // When
        val result = blocklistFilter.isBlocked("https://example.com/api/users")

        // Then
        result shouldBe false
    }

    @Test
    fun `isBlocked returns true for exact URL match`() {
        // Given
        blocklistFilter.blockUrl("https://example.com/api/users")

        // When
        val result = blocklistFilter.isBlocked("https://example.com/api/users")

        // Then
        result shouldBe true
    }

    @Test
    fun `isBlocked returns false for different URL`() {
        // Given
        blocklistFilter.blockUrl("https://example.com/api/users")

        // When
        val result = blocklistFilter.isBlocked("https://example.com/api/posts")

        // Then
        result shouldBe false
    }

    @Test
    fun `isBlocked returns true for blocked domain`() {
        // Given
        blocklistFilter.blockDomain("example.com")

        // When
        val result = blocklistFilter.isBlocked("https://example.com/api/users")

        // Then
        result shouldBe true
    }

    @Test
    fun `isBlocked returns true for subdomain of blocked domain`() {
        // Given
        blocklistFilter.blockDomain("example.com")

        // When
        val result = blocklistFilter.isBlocked("https://api.example.com/users")

        // Then
        result shouldBe true
    }

    @Test
    fun `isBlocked returns true for wildcard domain pattern`() {
        // Given
        blocklistFilter.blockDomain("*.example.com")

        // When - subdomain should match
        val resultSubdomain = blocklistFilter.isBlocked("https://api.example.com/users")

        // Then
        resultSubdomain shouldBe true
    }

    @Test
    fun `isBlocked returns false for different domain`() {
        // Given
        blocklistFilter.blockDomain("example.com")

        // When
        val result = blocklistFilter.isBlocked("https://other.com/api/users")

        // Then
        result shouldBe false
    }

    @Test
    fun `unblockUrl removes URL from blocklist`() {
        // Given
        blocklistFilter.blockUrl("https://example.com/api/users")

        // When
        blocklistFilter.unblockUrl("https://example.com/api/users")
        val result = blocklistFilter.isBlocked("https://example.com/api/users")

        // Then
        result shouldBe false
    }

    @Test
    fun `unblockDomain removes domain from blocklist`() {
        // Given
        blocklistFilter.blockDomain("example.com")

        // When
        blocklistFilter.unblockDomain("example.com")
        val result = blocklistFilter.isBlocked("https://example.com/api/users")

        // Then
        result shouldBe false
    }

    @Test
    fun `clearAll removes all blocklist entries`() {
        // Given
        blocklistFilter.blockUrl("https://example.com/api/users")
        blocklistFilter.blockDomain("blocked.com")

        // When
        blocklistFilter.clearAll()
        val resultUrl = blocklistFilter.isBlocked("https://example.com/api/users")
        val resultDomain = blocklistFilter.isBlocked("https://blocked.com/api")

        // Then
        resultUrl shouldBe false
        resultDomain shouldBe false
    }

    @Test
    fun `getBlockedUrls returns all blocked URLs`() {
        // Given
        blocklistFilter.blockUrl("https://example.com/api/users")
        blocklistFilter.blockUrl("https://example.com/api/posts")

        // When
        val blockedUrls = blocklistFilter.getBlockedUrls()

        // Then
        blockedUrls.size shouldBe 2
        blockedUrls.contains("https://example.com/api/users") shouldBe true
        blockedUrls.contains("https://example.com/api/posts") shouldBe true
    }

    @Test
    fun `getBlockedDomains returns all blocked domains`() {
        // Given
        blocklistFilter.blockDomain("example.com")
        blocklistFilter.blockDomain("blocked.com")

        // When
        val blockedDomains = blocklistFilter.getBlockedDomains()

        // Then
        blockedDomains.size shouldBe 2
        blockedDomains.contains("example.com") shouldBe true
        blockedDomains.contains("blocked.com") shouldBe true
    }

    @Test
    fun `isBlocked handles URL with port number`() {
        // Given
        blocklistFilter.blockDomain("example.com")

        // When
        val result = blocklistFilter.isBlocked("https://example.com:8080/api/users")

        // Then
        result shouldBe true
    }

    @Test
    fun `isBlocked handles HTTP and HTTPS`() {
        // Given
        blocklistFilter.blockDomain("example.com")

        // When
        val resultHttp = blocklistFilter.isBlocked("http://example.com/api/users")
        val resultHttps = blocklistFilter.isBlocked("https://example.com/api/users")

        // Then
        resultHttp shouldBe true
        resultHttps shouldBe true
    }
}
