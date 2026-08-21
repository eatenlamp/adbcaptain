package adb.captain.domain.usecase

import adb.captain.domain.model.Device
import adb.captain.domain.repository.AdbRepository
import adb.captain.domain.repository.BatteryDetails
import adb.captain.domain.repository.CpuInfo
import adb.captain.domain.repository.MemoryInfo
import adb.captain.domain.repository.ProcessInfo
import javax.inject.Inject

/**
 * UseCase для работы со списком устройств.
 */
class DeviceUseCase @Inject constructor(
    private val repository: AdbRepository
) {
    suspend fun getDevices(): List<Device> = repository.getDevices()
    suspend fun rebootDevice(serial: String) = repository.rebootDevice(serial)
    suspend fun rebootToRecovery(serial: String) = repository.rebootToRecovery(serial)
    suspend fun rebootToBootloader(serial: String) = repository.rebootToBootloader(serial)
    suspend fun takeScreenshot(serial: String) = repository.takeScreenshot(serial)

    suspend fun getShowTouches(): Boolean = repository.getShowTouches()
    suspend fun setShowTouches(enabled: Boolean) = repository.setShowTouches(enabled)
    suspend fun getAnimationScale(): Float = repository.getAnimationScale()
    suspend fun setAnimationScale(scale: Float) = repository.setAnimationScale(scale)
    suspend fun getUsbDebugging(): Boolean = repository.getUsbDebugging()
    suspend fun setUsbDebugging(enabled: Boolean) = repository.setUsbDebugging(enabled)
    suspend fun getOemUnlock(): Boolean = repository.getOemUnlock()
    suspend fun setOemUnlock(enabled: Boolean) = repository.setOemUnlock(enabled)
    suspend fun setWifiAdbEnabled(enabled: Boolean, port: Int = 5555) = repository.setWifiAdbEnabled(enabled, port)
    suspend fun getWifiAdbEnabled(): Boolean = repository.getWifiAdbEnabled()
    suspend fun getBatteryDetails(): BatteryDetails? = repository.getBatteryDetails()
    suspend fun getCpuInfo(): CpuInfo? = repository.getCpuInfo()
    suspend fun getMemoryInfo(): MemoryInfo? = repository.getMemoryInfo()
    suspend fun getTopProcesses(limit: Int = 20): List<ProcessInfo> = repository.getTopProcesses(limit)
    suspend fun setDemoMode(enabled: Boolean) = repository.setDemoMode(enabled)
    suspend fun getDemoMode(): Boolean = repository.getDemoMode()
    suspend fun createDiagnosticReport(): String = repository.createDiagnosticReport()
}
