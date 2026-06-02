package com.trace.app.proxy

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton

/**
 * Handles TLS interception with dynamic certificate generation.
 * Generates CA certificate on first launch and creates leaf certificates per hostname.
 */
class TlsInterceptor(
    private val context: Context
) {

    private val provider = BouncyCastleProvider()

    private var caKeyPair: KeyPair? = null
    private var caCertificate: X509Certificate? = null

    private val leafCache = ConcurrentHashMap<String, LeafCertificate>()

    companion object {
        private const val CA_ALIAS = "trace_ca"
        private const val CA_KEYSTORE = "trace_ca.p12"
        private const val CA_KEYSTORE_PASSWORD = "trace123"
        private const val CA_VALIDITY_YEARS = 10
        private const val LEAF_VALIDITY_DAYS = 1

        // CA Certificate Details
        private val CA_SUBJECT = X500Name("CN=Trace Proxy CA,O=Trace App,C=US")
    }

    init {
        Security.addProvider(provider)
        initializeCA()
    }

    private fun initializeCA() {
        try {
            // Try to load existing CA
            if (loadExistingCA()) {
                Timber.i("Loaded existing CA certificate")
                return
            }

            // Generate new CA
            generateCA()
            Timber.i("Generated new CA certificate")

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize CA")
        }
    }

    private fun loadExistingCA(): Boolean {
        return try {
            val keystoreFile = File(context.filesDir, CA_KEYSTORE)
            if (!keystoreFile.exists()) return false

            val keyStore = KeyStore.getInstance("PKCS12", provider)
            FileInputStream(keystoreFile).use { fis ->
                keyStore.load(fis, CA_KEYSTORE_PASSWORD.toCharArray())
            }

            val key = keyStore.getKey(CA_ALIAS, CA_KEYSTORE_PASSWORD.toCharArray())
            val cert = keyStore.getCertificate(CA_ALIAS) as? X509Certificate

            if (key != null && cert != null) {
                caKeyPair = KeyPair(cert.publicKey, key as PrivateKey)
                caCertificate = cert
                true
            } else {
                false
            }

        } catch (e: Exception) {
            Timber.e(e, "Error loading existing CA")
            false
        }
    }

    private fun generateCA() {
        // Generate RSA key pair
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", provider)
        keyPairGenerator.initialize(2048)
        caKeyPair = keyPairGenerator.generateKeyPair()

        // Build CA certificate
        val builder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            CA_SUBJECT,
            BigInteger.valueOf(System.currentTimeMillis()),
            Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000), // Valid from yesterday
            Date(System.currentTimeMillis() + (365L * CA_VALIDITY_YEARS * 24 * 60 * 60 * 1000)),
            CA_SUBJECT,
            caKeyPair!!.public
        )

        // Self-sign the certificate
        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .setProvider(provider)
            .build(caKeyPair!!.private)

        val certHolder: X509CertificateHolder = builder.build(signer)
        caCertificate = JcaX509CertificateConverter()
            .setProvider(provider)
            .getCertificate(certHolder)

        // Save to keystore
        saveCAToKeystore()
    }

    private fun saveCAToKeystore() {
        try {
            val keyStore = KeyStore.getInstance("PKCS12", provider)
            keyStore.load(null, null)

            keyStore.setKeyEntry(
                CA_ALIAS,
                caKeyPair!!.private,
                CA_KEYSTORE_PASSWORD.toCharArray(),
                arrayOf<Certificate>(caCertificate!!)
            )

            val keystoreFile = File(context.filesDir, CA_KEYSTORE)
            FileOutputStream(keystoreFile).use { fos ->
                keyStore.store(fos, CA_KEYSTORE_PASSWORD.toCharArray())
            }

        } catch (e: Exception) {
            Timber.e(e, "Error saving CA to keystore")
        }
    }

    /**
     * Generates a leaf certificate for a specific hostname.
     * Uses the CA certificate to sign the leaf certificate.
     */
    fun generateLeafCertificate(hostname: String): LeafCertificate? {
        // Check cache first
        leafCache[hostname]?.let { leaf ->
            if (!leaf.isExpired()) {
                return leaf
            }
            leafCache.remove(hostname)
        }

        return try {
            val ca = caCertificate ?: return null
            val caKey = caKeyPair ?: return null

            // Generate leaf key pair
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA", provider)
            keyPairGenerator.initialize(2048)
            val leafKeyPair = keyPairGenerator.generateKeyPair()

            // Build leaf certificate
            val subject = X500Name("CN=$hostname")
            val builder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
                ca,
                BigInteger.valueOf(System.currentTimeMillis()),
                Date(System.currentTimeMillis() - 60 * 1000), // Valid from 1 min ago
                Date(System.currentTimeMillis() + (LEAF_VALIDITY_DAYS * 24 * 60 * 60 * 1000L)),
                subject,
                leafKeyPair.public
            )

            // Sign with CA private key
            val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider(provider)
                .build(caKey.private)

            val certHolder = builder.build(signer)
            val leafCert = JcaX509CertificateConverter()
                .setProvider(provider)
                .getCertificate(certHolder)

            val leaf = LeafCertificate(
                certificate = leafCert,
                keyPair = leafKeyPair,
                hostname = hostname,
                expiresAt = System.currentTimeMillis() + (LEAF_VALIDITY_DAYS * 24 * 60 * 60 * 1000L)
            )

            leafCache[hostname] = leaf
            Timber.d("Generated leaf certificate for $hostname")

            leaf

        } catch (e: Exception) {
            Timber.e(e, "Error generating leaf certificate for $hostname")
            null
        }
    }

    fun exportCACertificate(): File? {
        return try {
            val cert = caCertificate ?: return null
            val exportsDir = File(context.filesDir, "exports")
            if (!exportsDir.exists()) exportsDir.mkdirs()
            val certFile = File(exportsDir, "trace_ca.pem")

            // Write PEM format (Base64 with headers)
            val base64 = android.util.Base64.encodeToString(cert.encoded, android.util.Base64.DEFAULT)
            val pem = "-----BEGIN CERTIFICATE-----" + "\n" + base64 + "-----END CERTIFICATE-----" + "\n"

            FileOutputStream(certFile).use { fos ->
                fos.write(pem.toByteArray())
            }

            Timber.i("Exported CA certificate to ${certFile.absolutePath}")
            certFile

        } catch (e: Exception) {
            Timber.e(e, "Error exporting CA certificate")
            null
        }
    }

    /**
     * Returns true if CA certificate has been generated.
     */
    fun hasCACertificate(): Boolean {
        return caCertificate != null
    }

    data class LeafCertificate(
        val certificate: X509Certificate,
        val keyPair: KeyPair,
        val hostname: String,
        val expiresAt: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() > expiresAt
        }
    }
}
