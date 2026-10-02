package adb.captain.domain.model

/**
 * Сохранённое устройство для подключения по ADB over Wi-Fi.
 */
data class SavedAdbDevice(
    val name: String,
    val host: String,
    val port: Int,
    val autoReconnect: Boolean = false
) {
    val endpoint: String get() = "$host:$port"
}
