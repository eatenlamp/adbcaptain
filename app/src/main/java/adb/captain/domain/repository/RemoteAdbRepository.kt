package adb.captain.domain.repository

import adb.captain.domain.model.SavedAdbDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Подключение к внешнему ADB-демону по Wi-Fi (ADB over TCP / Wireless debugging).
 */
interface RemoteAdbRepository {
    val connectionState: StateFlow<RemoteAdbState>

    fun savedDevices(): Flow<List<SavedAdbDevice>>
    suspend fun saveDevice(device: SavedAdbDevice)
    suspend fun removeDevice(host: String, port: Int)

    /** Сопряжение с устройством (Android 11+, шестизначный код). */
    suspend fun pair(host: String, port: Int, code: String): Result<Unit>

    suspend fun connect(host: String, port: Int): Result<Unit>
    suspend fun disconnect()

    /** Выполнить shell-команду на подключённом устройстве. */
    suspend fun shell(command: String): Result<String>

    /** Переподключиться к сохранённому устройству с флагом autoReconnect. */
    suspend fun autoReconnect()
}

sealed interface RemoteAdbState {
    data object Disconnected : RemoteAdbState
    data object Connecting : RemoteAdbState
    data class Connected(val host: String, val port: Int) : RemoteAdbState
}
