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
    val sharedUserId: String? = null
)

enum class BloatwareStatus {
    SAFE_TO_DELETE,
    NOT_SAFE_TO_DELETE,
    DELETE_IF_NEEDED,
    UNKNOWN
}
