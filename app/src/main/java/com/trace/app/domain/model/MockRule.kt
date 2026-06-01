package com.trace.app.domain.model

/**
 * Mock rule for overriding HTTP responses.
 * Supports URL pattern matching with wildcards.
 */
data class MockRule(
    val id: Long = 0,
    val urlPattern: String,
    val method: String? = null,
    val responseStatusCode: Int = 200,
    val responseHeaders: Map<String, String>? = null,
    val responseBody: String? = null,
    val delayMs: Long = 0,
    val isEnabled: Boolean = true,
    val priority: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val description: String? = null
) {
    /**
     * Checks if this rule matches the given URL and method.
     * Wildcard * matches any characters.
     */
    fun matches(url: String, method: String): Boolean {
        if (!isEnabled) return false

        // Check method filter
        if (this.method != null && this.method != method) return false

        // Convert wildcard pattern to regex
        val regex = urlPattern
            .replace("*", ".*")
            .replace("?", ".")
            .toRegex()

        return regex.matches(url)
    }
}
