package adb.captain.domain.repository

import adb.captain.domain.model.AppDetails
import adb.captain.domain.model.AppInfo
import adb.captain.domain.model.Device
import adb.captain.domain.model.FileEntry
import adb.captain.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Интерфейс для взаимодействия с ADB через Shizuku.
 */
interface AdbRepository {
    suspend fun executeCommand(command: String): String
    fun executeStreamingCommand(command: String): Flow<String>

    suspend fun getDevices(): List<Device>
    suspend fun getApps(system: Boolean? = null): List<AppInfo>
    suspend fun getAppDetails(packageName: String): AppDetails?

    fun streamLogcat(level: String? = null, filter: String? = null): Flow<LogEntry>

    suspend fun forceStopApp(packageName: String)
    suspend fun clearAppData(packageName: String)
    suspend fun uninstallApp(packageName: String)
    suspend fun toggleApp(packageName: String, enable: Boolean)
    suspend fun launchApp(packageName: String, activity: String? = null): String
    suspend fun exportApk(packageName: String): String

    suspend fun listFiles(path: String): List<FileEntry>
    suspend fun deleteFile(path: String)
    suspend fun createDirectory(path: String)

    suspend fun getShowTouches(): Boolean
    suspend fun setShowTouches(enabled: Boolean)
    suspend fun getAnimationScale(): Float
    suspend fun setAnimationScale(scale: Float)
    suspend fun getUsbDebugging(): Boolean
    suspend fun setUsbDebugging(enabled: Boolean)
    suspend fun getOemUnlock(): Boolean
    suspend fun setOemUnlock(enabled: Boolean)

    suspend fun takeScreenshot(deviceSerial: String): String
    suspend fun rebootDevice(deviceSerial: String)
    suspend fun rebootToRecovery(deviceSerial: String)
    suspend fun rebootToBootloader(deviceSerial: String)

    suspend fun sideloadApk(apkFile: File): String
    suspend fun installApkAtPath(remotePath: String): String
    suspend fun startScreenRecording(): String
    suspend fun stopScreenRecording()
    suspend fun wakeDevice()
    suspend fun dismissKeyguard()
    suspend fun setStayAwake(enabled: Boolean)
    suspend fun setWifiEnabled(enabled: Boolean)
    suspend fun setBluetoothEnabled(enabled: Boolean)
    suspend fun setAirplaneMode(enabled: Boolean)
    suspend fun setMediaVolume(level: Int)
    suspend fun sendTextInput(text: String)
    suspend fun openUrl(url: String)
    suspend fun setWifiAdbEnabled(enabled: Boolean, port: Int = 5555)
    suspend fun getWifiAdbEnabled(): Boolean

    suspend fun getBatteryDetails(): BatteryDetails?
    suspend fun getCpuInfo(): CpuInfo?
    suspend fun getMemoryInfo(): MemoryInfo?
    suspend fun getTopProcesses(limit: Int): List<ProcessInfo>
    suspend fun setDemoMode(enabled: Boolean)
    suspend fun getDemoMode(): Boolean

    suspend fun createDiagnosticReport(): String

    suspend fun runInputMacro(script: String): String
}

data class BatteryDetails(
    val level: Int,
    val status: String,
    val health: String,
    val technology: String,
    val temperature: Int,
    val voltage: Int,
    val current: Int,
    val capacity: Int,
    val plugged: String,
    val chargeCounter: Int? = null
)

data class CpuInfo(
    val totalUsage: Float,
    val perCoreUsage: List<Float>,
    val frequency: List<Long>,
    val governor: List<String>
)

data class MemoryInfo(
    val totalMem: Long,
    val availableMem: Long,
    val usedMem: Long,
    val freeMem: Long,
    val swapTotal: Long,
    val swapFree: Long
)

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val cpuUsage: Float,
    val memoryUsage: Long,
    val state: String
)
