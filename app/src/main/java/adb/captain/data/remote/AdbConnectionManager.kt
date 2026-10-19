/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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
import java.security.KeyStore
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
    @param:ApplicationContext private val appContext: Context
) : AbsAdbConnectionManager() {

    private val privateKey: PrivateKey
    private val certificate: Certificate

    init {
        val certFile = File(appContext.filesDir, CERT_FILE)
        val legacyKeyFile = File(appContext.filesDir, KEY_FILE)
        val secureKeyFile = File(appContext.filesDir, SECURE_KEY_FILE)

        // The envelope written by this version wins, but a key pair stored by any older
        // install is still read verbatim, so upgrading never forces a re-pair.
        val secure = loadSecureKeyPair(secureKeyFile, certFile)
        val legacy = if (secure == null) loadKeyPair(legacyKeyFile, certFile) else null
        val loaded = secure ?: legacy

        if (loaded != null) {
            privateKey = loaded.first
            certificate = loaded.second
            if (legacy != null) {
                // Move an existing plaintext key behind the Android KeyStore. The encrypted
                // copy is verified before the old file is removed, and any failure here is
                // non fatal: the key stays where it was and pairing keeps working.
                runCatching { storeSecureKeyPair(secureKeyFile, privateKey, certificate) }
                    .onSuccess {
                        legacyKeyFile.delete()
                        Log.i(TAG, "Stored ADB key migrated to the Android KeyStore")
                    }
                    .onFailure { Log.w(TAG, "Keeping the ADB key in its previous form", it) }
            }
        } else {
            val generated = generateKeyPair()
            privateKey = generated.first
            certificate = generated.second
            runCatching {
                storeSecureKeyPair(secureKeyFile, privateKey, certificate)
                certFile.writeBytes(certificate.encoded)
            }.onFailure {
                Log.w(TAG, "Storing the ADB key without encryption", it)
                runCatching {
                    legacyKeyFile.writeBytes(privateKey.encoded)
                    certFile.writeBytes(certificate.encoded)
                }
            }
        }
    }

    /**
     * Wraps the ADB private key in AES-GCM with a key that lives in the Android KeyStore,
     * so the bytes on disk are useless on their own. Layout is a one byte IV length, the
     * IV, then the ciphertext.
     */
    private fun storeSecureKeyPair(keyFile: File, privateKey: PrivateKey, certificate: Certificate) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val ciphertext = cipher.doFinal(privateKey.encoded)
        val iv = cipher.iv
        keyFile.outputStream().use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(ciphertext)
        }
        File(appContext.filesDir, CERT_FILE).writeBytes(certificate.encoded)
        // Read it straight back, so a truncated or miswritten file is caught here and
        // never becomes the only copy of the key.
        val verified = loadSecureKeyPair(keyFile, File(appContext.filesDir, CERT_FILE))
        check(verified != null && verified.first == privateKey) { "secure ADB key store failed verification" }
    }

    private fun loadSecureKeyPair(keyFile: File, certFile: File): Pair<PrivateKey, Certificate>? {
        if (!keyFile.exists() || !certFile.exists()) return null
        return runCatching {
            val stored = keyFile.readBytes()
            require(stored.size > 1) { "secure key file is empty" }
            val ivSize = stored[0].toInt()
            require(ivSize in 1..32 && stored.size > ivSize + 1) { "secure key file is malformed" }
            val iv = stored.copyOfRange(1, ivSize + 1)
            val ciphertext = stored.copyOfRange(ivSize + 1, stored.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            val key = KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePrivate(PKCS8EncodedKeySpec(cipher.doFinal(ciphertext)))
            val cert = ByteArrayInputStream(certFile.readBytes()).use {
                CertificateFactory.getInstance("X.509").generateCertificate(it)
            }
            key to cert
        }.onFailure { Log.w(TAG, "Unable to decrypt the stored ADB key pair", it) }.getOrNull()
    }

    /**
     * The wrapping key is created on first use and never leaves the KeyStore. If the user
     * changes their screen lock the KeyStore entry can be invalidated, in which case this
     * throws and the caller falls back to regenerating a key pair.
     */
    private fun wrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(WRAPPING_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(AES_ALGORITHM, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                WRAPPING_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(WRAPPING_KEY_BITS)
                .build()
        )
        return generator.generateKey()
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
        private const val SECURE_KEY_FILE = "wireless_adb_key.pk8.enc"
        private const val CERT_FILE = "wireless_adb_cert.der"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val WRAPPING_KEY_ALIAS = "adbcaptain_wireless_adb_key"
        private const val WRAPPING_KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val KEY_ALGORITHM = "RSA"
        private const val AES_ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 2048
        private const val CERT_VALIDITY_MS = 10L * 365 * 24 * 60 * 60 * 1000
    }
}
