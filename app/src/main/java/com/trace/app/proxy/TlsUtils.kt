package com.trace.app.proxy

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.StringWriter
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.inject.Singleton

@Singleton
object TlsUtils {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    /**
     * Creates an SSLContext configured with a leaf certificate for server-side TLS.
     * Used when proxy terminates TLS from client (MITM).
     */
    fun createServerSSLContext(leafCert: TlsInterceptor.LeafCertificate): SSLContext {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        keyStore.setKeyEntry(
            "leaf",
            leafCert.keyPair.private,
            "".toCharArray(),
            arrayOf(leafCert.certificate)
        )

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, "".toCharArray())

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, null, null)

        return sslContext
    }

    /**
     * Creates a client SSLContext that trusts all certificates.
     * Used when proxy connects to real server.
     * WARNING: Only use for debugging/proxy purposes!
     */
    fun createTrustAllClientSSLContext(): SSLContext {
        val trustManager = object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)
        return sslContext
    }

    /**
     * Exports certificate as PEM string for debugging.
     */
    fun certificateToPem(cert: X509Certificate): String {
        val sw = StringWriter()
        JcaPEMWriter(sw).use { writer ->
            writer.writeObject(JcaMiscPEMGenerator(cert))
        }
        return sw.toString()
    }

    /**
     * Exports private key as PKCS#8 PEM string.
     */
    fun privateKeyToPem(key: PrivateKey): String {
        val sw = StringWriter()
        JcaPEMWriter(sw).use { writer ->
            writer.writeObject(JcaMiscPEMGenerator(key))
        }
        return sw.toString()
    }
}
