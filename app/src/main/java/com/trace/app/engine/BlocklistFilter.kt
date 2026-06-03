package com.trace.app.engine

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks URLs against a blocklist and returns 502 for blocked requests.
 * Supports exact URL matching and wildcard domain patterns.
 */
@Singleton
class BlocklistFilter @Inject constructor() {

    private val blockedUrls = mutableSetOf<String>()
    private val blockedDomains = mutableSetOf<String>()

    /**
     * Checks if a URL should be blocked.
     */
    fun isBlocked(url: String): Boolean {
        // Check exact URL match
        if (blockedUrls.contains(url)) {
            Timber.d("URL blocked (exact match): $url")
            return true
        }

        // Check domain patterns
        val host = extractHost(url) ?: return false

        for (domain in blockedDomains) {
            if (matchesDomain(host, domain)) {
                Timber.d("URL blocked (domain match): $url -> $domain")
                return true
            }
        }

        return false
    }

    /**
     * Adds a URL to the blocklist.
     */
    fun blockUrl(url: String) {
        blockedUrls.add(url)
        Timber.d("Blocked URL: $url")
    }

    /**
     * Adds a domain to the blocklist.
     * Supports wildcards: *.example.com matches all subdomains.
     */
    fun blockDomain(domain: String) {
        blockedDomains.add(domain)
        Timber.d("Blocked domain: $domain")
    }

    /**
     * Removes a URL from the blocklist.
     */
    fun unblockUrl(url: String) {
        blockedUrls.remove(url)
        Timber.d("Unblocked URL: $url")
    }

    /**
     * Removes a domain from the blocklist.
     */
    fun unblockDomain(domain: String) {
        blockedDomains.remove(domain)
        Timber.d("Unblocked domain: $domain")
    }

    /**
     * Clears all blocklist entries.
     */
    fun clearAll() {
        blockedUrls.clear()
        blockedDomains.clear()
        Timber.d("Cleared all blocklist entries")
    }

    /**
     * Returns all blocked URLs.
     */
    fun getBlockedUrls(): Set<String> = blockedUrls.toSet()

    /**
     * Returns all blocked domains.
     */
    fun getBlockedDomains(): Set<String> = blockedDomains.toSet()

    /**
     * Extracts the host from a URL.
     */
    private fun extractHost(url: String): String? {
        return try {
            // Remove protocol
            var host = url
            if (host.contains("://")) {
                host = host.substringAfter("://")
            }

            // Remove path and query
            host = host.substringBefore("/")
            host = host.substringBefore(":")

            host.ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a host matches a domain pattern.
     * Supports wildcards: *.example.com
     */
    private fun matchesDomain(host: String, pattern: String): Boolean {
        // Exact match
        if (host == pattern) return true

        // Wildcard match: *.example.com
        if (pattern.startsWith("*.")) {
            val domain = pattern.substring(2)
            // Host should end with the domain or be the domain itself
            return host == domain || host.endsWith(".$domain")
        }

        // Host ends with pattern (subdomain match)
        return host.endsWith(".$pattern")
    }
}
