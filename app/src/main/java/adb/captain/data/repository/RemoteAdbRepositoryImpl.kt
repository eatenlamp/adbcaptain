package adb.captain.data.repository

import adb.captain.data.local.SettingsManager
import adb.captain.data.remote.AdbConnectionManager
import adb.captain.domain.model.SavedAdbDevice
import adb.captain.domain.repository.RemoteAdbRepository
import adb.captain.domain.repository.RemoteAdbState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAdbRepositoryImpl @Inject constructor(
    private val manager: AdbConnectionManager,
    private val settingsManager: SettingsManager
) : RemoteAdbRepository {

    private val _connectionState = MutableStateFlow<RemoteAdbState>(RemoteAdbState.Disconnected)
    override val connectionState: StateFlow<RemoteAdbState> = _connectionState.asStateFlow()

    init {
        manager.setApi(android.os.Build.VERSION.SDK_INT)
        manager.setTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }

    override fun savedDevices(): Flow<List<SavedAdbDevice>> =
        settingsManager.savedAdbDevicesRaw.map(::decode)

    override suspend fun saveDevice(device: SavedAdbDevice) {
        val current = decode(settingsManager.savedAdbDevicesRaw.first())
            .filterNot { it.host == device.host && it.port == device.port }
            .plus(device)
        settingsManager.setSavedAdbDevicesRaw(encode(current))
    }

    override suspend fun removeDevice(host: String, port: Int) {
        val current = decode(settingsManager.savedAdbDevicesRaw.first())
            .filterNot { it.host == host && it.port == port }
        settingsManager.setSavedAdbDevicesRaw(encode(current))
    }

    override suspend fun pair(host: String, port: Int, code: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                manager.pair(host.trim(), port, code.trim())
                Unit
            }
        }

    override suspend fun connect(host: String, port: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                _connectionState.value = RemoteAdbState.Connecting
                val connected = manager.connect(host.trim(), port)
                if (connected || manager.isConnected) {
                    _connectionState.value = RemoteAdbState.Connected(host.trim(), port)
                    saveDevice(SavedAdbDevice(host.trim(), host.trim(), port))
                } else {
                    error("Connection refused")
                }
            }.onFailure {
                _connectionState.value = RemoteAdbState.Disconnected
            }
        }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching { manager.disconnect() }
        }
        _connectionState.value = RemoteAdbState.Disconnected
    }

    override suspend fun shell(command: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                withTimeout(SHELL_TIMEOUT_MS) {
                    val stream = manager.openStream("shell:${command.trim()}")
                    val output = stream.openInputStream().bufferedReader().use { it.readText() }
                    runCatching { stream.close() }
                    output
                }
            }
        }

    override suspend fun autoReconnect() {
        val candidate = decode(settingsManager.savedAdbDevicesRaw.first())
            .firstOrNull { it.autoReconnect } ?: return
        connect(candidate.host, candidate.port)
    }

    private fun encode(devices: List<SavedAdbDevice>): String =
        devices.joinToString("\n") { device ->
            listOf(
                device.name.replace("\t", " "),
                device.host,
                device.port.toString(),
                if (device.autoReconnect) "1" else "0"
            ).joinToString("\t")
        }

    private fun decode(raw: String): List<SavedAdbDevice> =
        raw.lines().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split("\t")
            if (parts.size < 3) return@mapNotNull null
            val port = parts[2].toIntOrNull() ?: return@mapNotNull null
            SavedAdbDevice(
                name = parts[0].ifBlank { parts[1] },
                host = parts[1],
                port = port,
                autoReconnect = parts.getOrNull(3) == "1"
            )
        }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val SHELL_TIMEOUT_MS = 20_000L
    }
}
