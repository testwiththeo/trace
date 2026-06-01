package com.trace.app.domain.model

/**
 * Configuration for the local proxy server.
 */
data class ProxyConfig(
    val port: Int = 0, // 0 = random port
    val certPath: String? = null,
    val excludedApps: List<String> = emptyList()
) {
    companion object {
        const val DEFAULT_PORT = 8080
        const val LOCALHOST = "127.0.0.1"
    }
}
