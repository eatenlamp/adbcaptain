package adb.captain.presentation.screens.sideload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.usecase.SideloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SideloadViewModel @Inject constructor(
    private val useCase: SideloadUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SideloadUiState())
    val uiState: StateFlow<SideloadUiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null

    fun installApk(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInstalling = true, installResult = null) }
            val result = withContextIO {
                val file = File(context.cacheDir, "sideload_${System.currentTimeMillis()}.apk")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    useCase.sideloadApk(file)
                } finally {
                    file.delete()
                }
            }
            _uiState.update { it.copy(isInstalling = false, installResult = result) }
        }
    }

    fun clearInstallResult() {
        _uiState.update { it.copy(installResult = null) }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _uiState.update { it.copy(isRecording = true, recordingSeconds = 0) }
        recordingJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(recordingSeconds = it.recordingSeconds + 1) }
            }
        }
        viewModelScope.launch { useCase.startScreenRecording() }
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        viewModelScope.launch { useCase.stopScreenRecording() }
        _uiState.update { it.copy(isRecording = false, recordingSeconds = 0) }
    }

    fun takeScreenshot() {
        viewModelScope.launch {
            val path = withContextIO { useCase.takeScreenshot() }
            _uiState.update { it.copy(screenshotPath = path) }
        }
    }

    fun clearScreenshotPath() {
        _uiState.update { it.copy(screenshotPath = null) }
    }

    fun wakeDevice() {
        viewModelScope.launch { withContextIO { useCase.wakeDevice() } }
    }

    fun unlockDevice() {
        viewModelScope.launch { withContextIO { useCase.dismissKeyguard() } }
    }

    fun setStayAwake(enabled: Boolean) {
        _uiState.update { it.copy(stayAwake = enabled) }
        viewModelScope.launch { withContextIO { useCase.setStayAwake(enabled) } }
    }

    fun setWifiEnabled(enabled: Boolean) {
        _uiState.update { it.copy(wifiEnabled = enabled) }
        viewModelScope.launch { withContextIO { useCase.setWifiEnabled(enabled) } }
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        _uiState.update { it.copy(bluetoothEnabled = enabled) }
        viewModelScope.launch { withContextIO { useCase.setBluetoothEnabled(enabled) } }
    }

    fun setAirplaneMode(enabled: Boolean) {
        _uiState.update { it.copy(airplaneMode = enabled) }
        viewModelScope.launch { withContextIO { useCase.setAirplaneMode(enabled) } }
    }

    fun setMediaVolume(level: Int) {
        _uiState.update { it.copy(volumeLevel = level) }
        viewModelScope.launch { withContextIO { useCase.setMediaVolume(level) } }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { withContextIO { useCase.sendTextInput(text) } }
    }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch { withContextIO { useCase.openUrl(url) } }
    }

    fun startOverlay() {
        val intent = android.content.Intent(context, adb.captain.service.OverlayService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }

    fun stopOverlay() {
        context.stopService(android.content.Intent(context, adb.captain.service.OverlayService::class.java))
    }

    fun runMacro(script: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMacroRunning = true) }
            val result = withContextIO { useCase.runInputMacro(script) }
            _uiState.update { it.copy(isMacroRunning = false, macroResult = result) }
        }
    }

    fun clearMacroResult() {
        _uiState.update { it.copy(macroResult = null) }
    }
}

private suspend fun <T> withContextIO(block: suspend () -> T): T =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }

data class SideloadUiState(
    val isInstalling: Boolean = false,
    val installResult: String? = null,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val screenshotPath: String? = null,
    val stayAwake: Boolean = false,
    val wifiEnabled: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val airplaneMode: Boolean = false,
    val volumeLevel: Int = 70,
    val isMacroRunning: Boolean = false,
    val macroResult: String? = null
)
