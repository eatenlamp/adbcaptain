/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.ipc

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import adb.captain.BuildConfig
import java.security.MessageDigest

/**
 * Gate for [ExecutionService].
 *
 * The `normal` IPC permission is declared by any caller itself, so it only
 * keeps honest apps honest. The real check is here: the calling package name
 * must match ADB Orchestra, and - once the pins below are filled in - its
 * signing certificate must match too.
 */
object CallerVerifier {

    const val COMPANION_PACKAGE = "adb.orch"

    /**
     * SHA-256 (lowercase hex) of the ADB Orchestra signing certificates.
     * Empty until the companion is signed: debug builds are then allowed so
     * development is not blocked. Add the release and debug certs before
     * shipping, otherwise release builds refuse every caller.
     */
    private val ALLOWED_CERT_SHA256 = setOf<String>()

    fun isAllowed(context: Context, callerUid: Int): Boolean {
        val pm = context.packageManager
        val packages = pm.getPackagesForUid(callerUid) ?: return false
        if (packages.none { it == COMPANION_PACKAGE }) return false
        if (ALLOWED_CERT_SHA256.isEmpty()) return BuildConfig.DEBUG
        return packages.any { pkg -> certSha256(pm, pkg).any { it in ALLOWED_CERT_SHA256 } }
    }

    private fun certSha256(pm: PackageManager, packageName: String): List<String> = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners?.map { sha256(it.toByteArray()) } ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures?.map { sha256(it.toByteArray()) } ?: emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
