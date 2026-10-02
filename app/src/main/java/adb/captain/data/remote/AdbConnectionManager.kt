package adb.captain.data.remote

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton
import android.sun.security.x509.AlgorithmId
import android.sun.security.x509.CertificateAlgorithmId
import android.sun.security.x509.CertificateExtensions
import android.sun.security.x509.CertificateIssuerName
import android.sun.security.x509.CertificateSerialNumber
import android.sun.security.x509.CertificateSubjectName
import android.sun.security.x509.CertificateValidity
import android.sun.security.x509.CertificateVersion
import android.sun.security.x509.CertificateX509Key
import android.sun.security.x509.KeyIdentifier
import android.sun.security.x509.PrivateKeyUsageExtension
import android.sun.security.x509.SubjectKeyIdentifierExtension
import android.sun.security.x509.X500Name
import android.sun.security.x509.X509CertImpl
import android.sun.security.x509.X509CertInfo

/**
 * Обёртка над LibADB: генерирует/хранит клиентскую пару ключ-сертификат ADB,
 * общую для всех подключений, чтобы сопряжение сохранялось между запусками.
 */
@Singleton
class AdbConnectionManager @Inject constructor(
    @ApplicationContext context: Context
) : AbsAdbConnectionManager() {

    private val privateKey: PrivateKey
    private val certificate: Certificate

    init {
        val keyFile = File(context.filesDir, KEY_FILE)
        val certFile = File(context.filesDir, CERT_FILE)
        val loaded = loadKeyPair(keyFile, certFile)
        if (loaded != null) {
            privateKey = loaded.first
            certificate = loaded.second
        } else {
            val generated = generateKeyPair()
            privateKey = generated.first
            certificate = generated.second
            runCatching {
                keyFile.writeBytes(privateKey.encoded)
                certFile.writeBytes(certificate.encoded)
            }
        }
    }

    override fun getPrivateKey(): PrivateKey = privateKey

    override fun getCertificate(): Certificate = certificate

    override fun getDeviceName(): String = "ADB Captain"

    private fun loadKeyPair(keyFile: File, certFile: File): Pair<PrivateKey, Certificate>? {
        if (!keyFile.exists() || !certFile.exists()) return null
        return runCatching {
            val keyFactory = KeyFactory.getInstance("RSA")
            val key = keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = ByteArrayInputStream(certFile.readBytes()).use { certFactory.generateCertificate(it) }
            key to cert
        }.onFailure { Log.w(TAG, "Unable to load stored ADB key pair, regenerating", it) }.getOrNull()
    }

    private fun generateKeyPair(): Pair<PrivateKey, Certificate> {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(KEY_SIZE_BITS, SecureRandom.getInstance("SHA1PRNG"))
        val keyPair = generator.generateKeyPair()
        val privateKey = keyPair.private
        val publicKey = keyPair.public
        return privateKey to generateCertificate(privateKey, publicKey)
    }

    private fun generateCertificate(privateKey: PrivateKey, publicKey: PublicKey): Certificate {
        val algorithmName = "SHA512withRSA"
        val subject = "CN=ADB Captain"
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + CERT_VALIDITY_MS)

        val extensions = CertificateExtensions().apply {
            set(
                "SubjectKeyIdentifier",
                SubjectKeyIdentifierExtension(KeyIdentifier(publicKey).identifier)
            )
            set("PrivateKeyUsage", PrivateKeyUsageExtension(notBefore, notAfter))
        }
        val x500Name = X500Name(subject)
        val certInfo = X509CertInfo().apply {
            set("version", CertificateVersion(2))
            set("serialNumber", CertificateSerialNumber(Random().nextInt() and Int.MAX_VALUE))
            set("algorithmID", CertificateAlgorithmId(AlgorithmId.get(algorithmName)))
            set("subject", CertificateSubjectName(x500Name))
            set("key", CertificateX509Key(publicKey))
            set("validity", CertificateValidity(notBefore, notAfter))
            set("issuer", CertificateIssuerName(x500Name))
            set("extensions", extensions)
        }
        val certImpl = X509CertImpl(certInfo)
        certImpl.sign(privateKey, algorithmName)
        return certImpl
    }

    companion object {
        private const val TAG = "AdbConnectionManager"
        private const val KEY_FILE = "wireless_adb_key.pk8"
        private const val CERT_FILE = "wireless_adb_cert.der"
        private const val KEY_SIZE_BITS = 2048
        private const val CERT_VALIDITY_MS = 10L * 365 * 24 * 60 * 60 * 1000
    }
}
