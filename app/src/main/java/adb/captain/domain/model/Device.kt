package adb.captain.domain.model

/**
 * Модель подключенного устройства.
 */
data class Device(
    val serial: String,
    val model: String = "Unknown",
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val status: DeviceStatus = DeviceStatus.OFFLINE,
    val batteryLevel: Int = -1
)

enum class DeviceStatus {
    ONLINE, OFFLINE, UNAUTHORIZED, RECOVERY
}
