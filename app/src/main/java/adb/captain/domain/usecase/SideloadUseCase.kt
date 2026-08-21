package adb.captain.domain.usecase

import adb.captain.domain.repository.AdbRepository
import java.io.File
import javax.inject.Inject

/**
 * UseCase для установки APK и управления устройством.
 */
class SideloadUseCase @Inject constructor(
    private val repository: AdbRepository
) {
    suspend fun sideloadApk(apkFile: File): String = repository.sideloadApk(apkFile)
    suspend fun installApkAtPath(remotePath: String): String = repository.installApkAtPath(remotePath)
    suspend fun startScreenRecording(): String = repository.startScreenRecording()
    suspend fun stopScreenRecording() = repository.stopScreenRecording()
    suspend fun takeScreenshot(): String = repository.takeScreenshot("")
    suspend fun wakeDevice() = repository.wakeDevice()
    suspend fun dismissKeyguard() = repository.dismissKeyguard()
    suspend fun setStayAwake(enabled: Boolean) = repository.setStayAwake(enabled)
    suspend fun setWifiEnabled(enabled: Boolean) = repository.setWifiEnabled(enabled)
    suspend fun setBluetoothEnabled(enabled: Boolean) = repository.setBluetoothEnabled(enabled)
    suspend fun setAirplaneMode(enabled: Boolean) = repository.setAirplaneMode(enabled)
    suspend fun setMediaVolume(level: Int) = repository.setMediaVolume(level)
    suspend fun sendTextInput(text: String) = repository.sendTextInput(text)
    suspend fun openUrl(url: String) = repository.openUrl(url)
    suspend fun getShowTouches(): Boolean = repository.getShowTouches()
    suspend fun setShowTouches(enabled: Boolean) = repository.setShowTouches(enabled)
    suspend fun getAnimationScale(): Float = repository.getAnimationScale()
    suspend fun setAnimationScale(scale: Float) = repository.setAnimationScale(scale)
    suspend fun getUsbDebugging(): Boolean = repository.getUsbDebugging()
    suspend fun setUsbDebugging(enabled: Boolean) = repository.setUsbDebugging(enabled)
    suspend fun runInputMacro(script: String): String = repository.runInputMacro(script)
}
