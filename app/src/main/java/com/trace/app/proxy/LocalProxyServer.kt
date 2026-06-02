package com.trace.app.proxy

import com.trace.app.domain.repository.TrafficRepository
import com.trace.app.engine.BlocklistFilter
import com.trace.app.engine.DelayInjector
import com.trace.app.engine.MockEngine
import com.trace.app.engine.TrafficCapturer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local HTTP/HTTPS proxy using raw ServerSocket.
 * Required over Ktor because CONNECT + TLS MITM needs raw socket access.
 *
 * Flow:
 * - HTTP: Client → Proxy parses request → Forward → Save to DB → Return
 * - HTTPS: Client → CONNECT → 200 → TLS MITM → Decrypt → Inspect → Re-encrypt → Forward
 */
@Singleton
class LocalProxyServer @Inject constructor(
    private val trafficRepository: TrafficRepository,
    private val trafficCapturer: TrafficCapturer,
    private val mockEngine: MockEngine,
    private val blocklistFilter: BlocklistFilter,
    private val delayInjector: DelayInjector,
    private val tlsInterceptor: TlsInterceptor
) {

    private var serverSocket: ServerSocket? = null
    private var actualPort: Int = 0
    private val isRunning = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track active tunnels for cleanup
    private val activeTunnels = ConcurrentHashMap<String, Job>()

    private val _status = MutableStateFlow<ProxyStatus>(ProxyStatus.Stopped)
    val status: StateFlow<ProxyStatus> = _status

    sealed class ProxyStatus {
        object Stopped : ProxyStatus()
        object Starting : ProxyStatus()
        data class Running(val port: Int) : ProxyStatus()
        data class Error(val message: String) : ProxyStatus()
    }

    suspend fun start(): Int = withContext(Dispatchers.IO) {
        if (isRunning.get()) {
            Timber.w("Proxy already running on port $actualPort")
            return@withContext actualPort
        }

        _status.value = ProxyStatus.Starting

        try {
            serverSocket = ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"))
            actualPort = serverSocket!!.localPort
            isRunning.set(true)
            _status.value = ProxyStatus.Running(actualPort)
            Timber.i("Proxy started on port $actualPort")

            // Accept connections in background
            scope.launch {
                acceptConnections()
            }

            actualPort
        } catch (e: Exception) {
            Timber.e(e, "Failed to start proxy")
            _status.value = ProxyStatus.Error(e.message ?: "Unknown error")
            throw e
        }
    }

    private suspend fun acceptConnections() {
        while (isRunning.get()) {
            try {
                val clientSocket = serverSocket?.accept() ?: break
                clientSocket.soTimeout = 30_000

                // Handle each connection in its own coroutine
                scope.launch {
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Timber.e(e, "Error accepting connection")
                }
            }
        }
    }

    private suspend fun handleClient(clientSocket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val requestLine = reader.readLine() ?: return@withContext

            // Parse: METHOD URI HTTP/1.x
            val parts = requestLine.split(" ", limit = 3)
            if (parts.size < 3) {
                clientSocket.close()
                return@withContext
            }

            val method = parts[0]
            val uri = parts[1]
            val httpVersion = parts[2]

            // Read headers
            val headers = mutableMapOf<String, String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                val colonIdx = line!!.indexOf(':')
                if (colonIdx > 0) {
                    val key = line!!.substring(0, colonIdx).trim()
                    val value = line!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            if (method == "CONNECT") {
                handleConnect(clientSocket, reader, uri, headers)
            } else {
                handleHttpRequest(clientSocket, reader, method, uri, httpVersion, headers)
            }

        } catch (e: Exception) {
            Timber.e(e, "Error handling client")
            closeQuietly(clientSocket)
        }
    }

    // ──────────────────────────────────────────────
    //  CONNECT (HTTPS tunnel)
    // ──────────────────────────────────────────────

    private suspend fun handleConnect(
        clientSocket: Socket,
        reader: BufferedReader,
        uri: String,
        headers: Map<String, String>
    ) {
        val hostPort = uri.split(":", limit = 2)
        val hostname = hostPort.getOrNull(0) ?: ""
        val port = hostPort.getOrNull(1)?.toIntOrNull() ?: 443

        Timber.d("CONNECT $hostname:$port")

        val clientOut = clientSocket.getOutputStream()

        // Send 200 Connection Established
        clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
        clientOut.flush()

        if (!tlsInterceptor.hasCACertificate()) {
            Timber.w("No CA cert — falling back to plain relay for $hostname")
            plainRelay(clientSocket, hostname, port)
            return
        }

        // MITM: terminate TLS from client, establish TLS to server
        handleTlsMitm(clientSocket, hostname, port)
    }

    private suspend fun handleTlsMitm(
        clientSocket: Socket,
        hostname: String,
        port: Int
    ) {
        val startTime = System.currentTimeMillis()

        try {
            // Generate leaf cert for this hostname
            val leafCert = tlsInterceptor.generateLeafCertificate(hostname)
            if (leafCert == null) {
                Timber.e("Failed to generate leaf cert for $hostname")
                plainRelay(clientSocket, hostname, port)
                return
            }

            // Create SSLSocket for client side (server mode)
            val sslContext = TlsUtils.createServerSSLContext(leafCert)
            val clientSsl = sslContext.socketFactory.createSocket(
                clientSocket,
                clientSocket.inetAddress.hostName,
                clientSocket.port,
                true
            ) as javax.net.ssl.SSLSocket
            clientSsl.useClientMode = false
            clientSsl.startHandshake()

            Timber.d("TLS handshake with client OK: $hostname")

            // Connect to real server with TLS
            val serverSocket = java.net.Socket()
            serverSocket.connect(java.net.InetSocketAddress(hostname, port), 10_000)
            serverSocket.soTimeout = 30_000

            val serverSslContext = javax.net.ssl.SSLContext.getInstance("TLS")
            serverSslContext.init(null, null, null)
            val serverSsl = serverSslContext.socketFactory.createSocket(
                serverSocket, hostname, port, true
            ) as javax.net.ssl.SSLSocket
            serverSsl.useClientMode = true
            serverSsl.startHandshake()

            Timber.d("TLS handshake with server OK: $hostname")

            // Read decrypted request from client
            val clientIn = clientSsl.inputStream
            val clientOut2 = clientSsl.outputStream

            // Parse the HTTP request inside TLS
            val reqReader = BufferedReader(InputStreamReader(clientIn))
            val requestLine = reqReader.readLine() ?: run {
                closeQuietly(clientSsl)
                closeQuietly(serverSsl)
                return
            }

            val reqParts = requestLine.split(" ", limit = 3)
            val method = reqParts.getOrElse(0) { "GET" }
            val reqPath = reqParts.getOrElse(1) { "/" }

            val reqHeaders = mutableMapOf<String, String>()
            var line: String?
            while (reqReader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                val ci = line!!.indexOf(':')
                if (ci > 0) {
                    reqHeaders[line!!.substring(0, ci).trim()] = line!!.substring(ci + 1).trim()
                }
            }

            // Read request body if present
            val contentLength = reqHeaders["Content-Length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) {
                val buf = ByteArray(minOf(contentLength, 1_048_576))
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = clientIn.read(buf, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(buf, 0, totalRead)
            } else null

            val fullUrl = "https://$hostname$reqPath"

            // ── Blocklist ──
            if (blocklistFilter.isBlocked(fullUrl)) {
                val resp = "HTTP/1.1 502 Blocked\r\nContent-Length: 17\r\n\r\nBlocked by Trace"
                clientSsl.outputStream.write(resp.toByteArray())
                clientSsl.outputStream.flush()
                saveTraffic(
                    method, fullUrl, reqHeaders, body, 502, emptyMap(), "Blocked by Trace",
                    System.currentTimeMillis() - startTime
                )
                closeQuietly(clientSsl)
                closeQuietly(serverSsl)
                return
            }

            // ── Mock ──
            val mockResponse = mockEngine.evaluateMockRules(fullUrl, method)
            if (mockResponse != null) {
                if (mockResponse.delayMs > 0) delay(mockResponse.delayMs)

                val headerStr = mockResponse.headers.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
                val mockBody = mockResponse.body ?: ""
                val resp =
                    "HTTP/1.1 ${mockResponse.statusCode} Mocked\r\n$headerStr\r\nContent-Length: ${mockBody.toByteArray().size}\r\n\r\n$mockBody"
                clientSsl.outputStream.write(resp.toByteArray())
                clientSsl.outputStream.flush()
                saveTraffic(
                    method, fullUrl, reqHeaders, body, mockResponse.statusCode, mockResponse.headers, mockBody,
                    System.currentTimeMillis() - startTime
                )
                closeQuietly(clientSsl)
                closeQuietly(serverSsl)
                return
            }

            // ── Forward to real server ──
            val serverOut = serverSsl.outputStream
            val fwdRequest = buildString {
                append("$method $reqPath HTTP/1.1\r\n")
                reqHeaders.forEach { (k, v) ->
                    if (!k.equals("Proxy-Connection", ignoreCase = true)) {
                        append("$k: $v\r\n")
                    }
                }
                append("\r\n")
                if (body != null) append(body)
            }
            serverOut.write(fwdRequest.toByteArray())
            serverOut.flush()

            // Read response from server
            val serverIn = serverSsl.inputStream
            val serverReader = BufferedReader(InputStreamReader(serverIn))

            val respStatusLine = serverReader.readLine() ?: "HTTP/1.1 502 Bad Gateway"
            val respStatusParts = respStatusLine.split(" ", limit = 3)
            val respStatusCode = respStatusParts.getOrElse(1) { "502" }.toIntOrNull() ?: 502

            val respHeaders = mutableMapOf<String, String>()
            while (serverReader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                val ci = line!!.indexOf(':')
                if (ci > 0) {
                    respHeaders[line!!.substring(0, ci).trim()] = line!!.substring(ci + 1).trim()
                }
            }

            val respContentLength = respHeaders["Content-Length"]?.toIntOrNull() ?: 0
            val responseBody = if (respContentLength > 0) {
                val buf = ByteArray(minOf(respContentLength, 1_048_576))
                var totalRead = 0
                while (totalRead < respContentLength) {
                    val read = serverIn.read(buf, totalRead, respContentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(buf, 0, totalRead)
            } else null

            // ── Delay ──
            delayInjector.applyDelay(fullUrl, method)

            // ── Send response to client ──
            val headerStr = respHeaders.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
            val respBodyBytes = (responseBody ?: "").toByteArray()
            val resp = "$respStatusLine\r\n$headerStr\r\nContent-Length: ${respBodyBytes.size}\r\n\r\n"
            clientSsl.outputStream.write(resp.toByteArray())
            if (respBodyBytes.isNotEmpty()) {
                clientSsl.outputStream.write(respBodyBytes)
            }
            clientSsl.outputStream.flush()

            // ── Save traffic ──
            saveTraffic(
                method, fullUrl, reqHeaders, body, respStatusCode, respHeaders, responseBody,
                System.currentTimeMillis() - startTime
            )

            closeQuietly(clientSsl)
            closeQuietly(serverSsl)

        } catch (e: Exception) {
            Timber.e(e, "TLS MITM error for $hostname")
            plainRelay(clientSocket, hostname, port)
        }
    }

    // ──────────────────────────────────────────────
    //  HTTP request handling (plain text)
    // ──────────────────────────────────────────────

    private suspend fun handleHttpRequest(
        clientSocket: Socket,
        reader: BufferedReader,
        method: String,
        uri: String,
        httpVersion: String,
        headers: Map<String, String>
    ) {
        val startTime = System.currentTimeMillis()
        val fullUrl = if (uri.startsWith("http://")) uri else "http://${headers["Host"] ?: "localhost"}$uri"

        // Read request body
        val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
        val body = if (contentLength > 0) {
            val buf = ByteArray(minOf(contentLength, 1_048_576))
            val clientIn = clientSocket.getInputStream()
            var totalRead = 0
            while (totalRead < contentLength) {
                val read = clientIn.read(buf, totalRead, contentLength - totalRead)
                if (read == -1) break
                totalRead += read
            }
            String(buf, 0, totalRead)
        } else null

        val clientOut = clientSocket.getOutputStream()

        // ── Blocklist ──
        if (blocklistFilter.isBlocked(fullUrl)) {
            val resp = "HTTP/1.1 502 Blocked\r\nContent-Length: 17\r\n\r\nBlocked by Trace"
            clientOut.write(resp.toByteArray())
            clientOut.flush()
            saveTraffic(
                method, fullUrl, headers, body, 502, emptyMap(), "Blocked by Trace",
                System.currentTimeMillis() - startTime
            )
            closeQuietly(clientSocket)
            return
        }

        // ── Mock ──
        val mockResponse = mockEngine.evaluateMockRules(fullUrl, method)
        if (mockResponse != null) {
            if (mockResponse.delayMs > 0) delay(mockResponse.delayMs)
            val headerStr = mockResponse.headers.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
            val mockBody = mockResponse.body ?: ""
            val resp =
                "HTTP/1.1 ${mockResponse.statusCode} Mocked\r\n$headerStr\r\nContent-Length: ${mockBody.toByteArray().size}\r\n\r\n$mockBody"
            clientOut.write(resp.toByteArray())
            clientOut.flush()
            saveTraffic(
                method, fullUrl, headers, body, mockResponse.statusCode, mockResponse.headers, mockBody,
                System.currentTimeMillis() - startTime
            )
            closeQuietly(clientSocket)
            return
        }

        // ── Forward to real server ──
        try {
            val serverSocket = java.net.Socket()
            val targetHost = headers["Host"] ?: fullUrl.substringAfter("://").substringBefore("/")
            val targetPort = if (fullUrl.startsWith("https")) 443 else 80
            serverSocket.connect(java.net.InetSocketAddress(targetHost, targetPort), 10_000)
            serverSocket.soTimeout = 30_000

            val serverOut = serverSocket.getOutputStream()
            val serverIn = serverSocket.getInputStream()

            // Forward request
            val fwdRequest = buildString {
                val path = if (fullUrl.startsWith("http://")) {
                    fullUrl.removePrefix("http://").substringAfter("/")
                } else uri
                append("$method /$path HTTP/1.1\r\n")
                headers.forEach { (k, v) ->
                    if (!k.equals("Proxy-Connection", ignoreCase = true)) {
                        append("$k: $v\r\n")
                    }
                }
                append("\r\n")
                if (body != null) append(body)
            }
            serverOut.write(fwdRequest.toByteArray())
            serverOut.flush()

            // Read response
            val serverReader = BufferedReader(InputStreamReader(serverIn))
            val respStatusLine = serverReader.readLine() ?: "HTTP/1.1 502 Bad Gateway"
            val respParts = respStatusLine.split(" ", limit = 3)
            val respStatusCode = respParts.getOrElse(1) { "502" }.toIntOrNull() ?: 502

            val respHeaders = mutableMapOf<String, String>()
            var line: String?
            while (serverReader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                val ci = line!!.indexOf(':')
                if (ci > 0) {
                    respHeaders[line!!.substring(0, ci).trim()] = line!!.substring(ci + 1).trim()
                }
            }

            val respCL = respHeaders["Content-Length"]?.toIntOrNull() ?: 0
            val responseBody = if (respCL > 0) {
                val buf = ByteArray(minOf(respCL, 1_048_576))
                var totalRead = 0
                while (totalRead < respCL) {
                    val read = serverIn.read(buf, totalRead, respCL - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(buf, 0, totalRead)
            } else null

            // Delay
            delayInjector.applyDelay(fullUrl, method)

            // Send response to client
            val headerStr = respHeaders.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
            val respBodyBytes = (responseBody ?: "").toByteArray()
            val resp = "$respStatusLine\r\n$headerStr\r\nContent-Length: ${respBodyBytes.size}\r\n\r\n"
            clientOut.write(resp.toByteArray())
            if (respBodyBytes.isNotEmpty()) {
                clientOut.write(respBodyBytes)
            }
            clientOut.flush()

            saveTraffic(
                method, fullUrl, headers, body, respStatusCode, respHeaders, responseBody,
                System.currentTimeMillis() - startTime
            )

            closeQuietly(serverSocket)
        } catch (e: Exception) {
            Timber.e(e, "Error forwarding HTTP request to $fullUrl")
            val errResp = "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 27\r\n\r\nForwarding error: ${e.message}"
            clientOut.write(errResp.toByteArray())
            clientOut.flush()

            saveTraffic(
                method, fullUrl, headers, body, 502, emptyMap(), "Forwarding error",
                System.currentTimeMillis() - startTime
            )
        }

        closeQuietly(clientSocket)
    }

    // ──────────────────────────────────────────────
    //  Plain relay (no MITM, just tunnel bytes)
    // ──────────────────────────────────────────────

    private suspend fun plainRelay(clientSocket: Socket, hostname: String, port: Int) {
        try {
            val serverSocket = java.net.Socket()
            serverSocket.connect(java.net.InetSocketAddress(hostname, port), 10_000)

            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()
            val serverIn = serverSocket.getInputStream()
            val serverOut = serverSocket.getOutputStream()

            // Relay in both directions
            val job1 = scope.launch(Dispatchers.IO) { relay(clientIn, serverOut, "c→s") }
            val job2 = scope.launch(Dispatchers.IO) { relay(serverIn, clientOut, "s→c") }

            // Wait for either direction to finish
            joinAll(job1, job2)

            closeQuietly(serverSocket)
        } catch (e: Exception) {
            Timber.e(e, "Plain relay error for $hostname")
        } finally {
            closeQuietly(clientSocket)
        }
    }

    private fun relay(input: java.io.InputStream, output: java.io.OutputStream, tag: String) {
        try {
            val buf = ByteArray(8192)
            var n: Int
            while (input.read(buf).also { n = it } != -1) {
                output.write(buf, 0, n)
                output.flush()
            }
        } catch (e: Exception) {
            Timber.v("Relay ended ($tag): ${e.message}")
        }
    }

    // ──────────────────────────────────────────────
    //  Traffic save
    // ──────────────────────────────────────────────

    private suspend fun saveTraffic(
        method: String,
        url: String,
        requestHeaders: Map<String, String>,
        requestBody: String?,
        responseStatusCode: Int,
        responseHeaders: Map<String, String>,
        responseBody: String?,
        durationMs: Long
    ) {
        try {
            val captured = trafficCapturer.captureTraffic(
                method = method,
                url = url,
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseStatusCode = responseStatusCode,
                responseHeaders = responseHeaders,
                responseBody = responseBody,
                durationMs = durationMs,
                appPackage = "unknown",
                isHttps = url.startsWith("https", ignoreCase = true)
            )
            trafficRepository.insertTraffic(captured)
            Timber.d("Saved: $method $url → $responseStatusCode (${durationMs}ms)")
        } catch (e: Exception) {
            Timber.e(e, "Error saving traffic")
        }
    }

    // ──────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        activeTunnels.values.forEach { it.cancel() }
        activeTunnels.clear()
        try {
            serverSocket?.close()
            serverSocket = null
            scope.cancel()
            _status.value = ProxyStatus.Stopped
            Timber.i("Proxy stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping proxy")
        }
    }

    private fun closeQuietly(socket: Socket?) {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
    }
}
