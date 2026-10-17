/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/**
 * Модель установленного приложения.
 */
data class AppInfo(
    val packageName: String,
    val label: String = "",
    val versionName: String = "",
    val isSystem: Boolean = false,
    val enabled: Boolean = true,
    val size: String = "Unknown",
    val iconUri: String? = null,
    val safetyStatus: BloatwareStatus = BloatwareStatus.UNKNOWN
)

/**
 * Подробная информация о приложении.
 */
data class AppDetails(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Int,
    val targetSdk: Int,
    val minSdk: Int,
    val isSystem: Boolean,
    val enabled: Boolean,
    val installTime: Long,
    val updateTime: Long,
    val permissions: List<String>,
    val launchableActivities: List<String>,
    val apkPath: String,
    val dataDir: String,
    val uid: Int,
    val targetSandboxVersion: Int,
    val isDebuggable: Boolean,
    val isExternal: Boolean,
    val requestedPermissions: List<String>,
    val installPermissions: List<String>,
    val sharedUserId: String? = null,
    /** Activities, services, receivers and providers declared by the app. */
    val components: List<AppComponent> = emptyList(),
    /** Requested permissions split into dangerous/normal and grant state. */
    val permissionsDetailed: List<PermissionEntry> = emptyList(),
    /** Package that installed this app, null for sideloaded or system apps. */
    val installerPackageName: String? = null
)

/** One declared component as reported by `dumpsys package`. */
data class AppComponent(
    val type: String,
    val name: String,
    val permission: String? = null,
    val enabled: Boolean = true
)

/** A requested permission with its risk level and grant state. */
data class PermissionEntry(
    val name: String,
    val dangerous: Boolean,
    val granted: Boolean
)

enum class BloatwareStatus {
    SAFE_TO_DELETE,
    NOT_SAFE_TO_DELETE,
    DELETE_IF_NEEDED,
    UNKNOWN
}
