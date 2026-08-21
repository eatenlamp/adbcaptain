package adb.captain.presentation.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.Device
import adb.captain.domain.repository.BatteryDetails
import adb.captain.domain.repository.CpuInfo
import adb.captain.domain.repository.MemoryInfo
import adb.captain.domain.repository.ProcessInfo
import adb.captain.domain.usecase.DeviceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val useCase: DeviceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    private var monitorJob: Job? = null

    init {
        refreshDevices()
        loadGlobalSettings()
        refreshBattery()
    }

    fun refreshDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val devices = useCase.getDevices()
            _uiState.update { it.copy(devices = devices, isLoading = false) }
        }
        loadGlobalSettings()
    }

    fun loadGlobalSettings() {
        viewModelScope.launch {
            val showTouches = useCase.getShowTouches()
            val animScale = useCase.getAnimationScale()
            val usbDebugging = useCase.getUsbDebugging()
            val oemUnlock = useCase.getOemUnlock()
            val wifiAdb = useCase.getWifiAdbEnabled()
            val demoMode = useCase.getDemoMode()
            _uiState.update {
                it.copy(
                    showTouches = showTouches,
                    animationScale = animScale,
                    usbDebugging = usbDebugging,
                    oemUnlock = oemUnlock,
                    wifiAdb = wifiAdb,
                    demoMode = demoMode
                )
            }
        }
    }

    fun toggleShowTouches(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setShowTouches(enabled)
            _uiState.update { it.copy(showTouches = enabled) }
        }
    }

    fun setAnimationScale(scale: Float) {
        viewModelScope.launch {
            useCase.setAnimationScale(scale)
            _uiState.update { it.copy(animationScale = scale) }
        }
    }

    fun toggleUsbDebugging(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setUsbDebugging(enabled)
            _uiState.update { it.copy(usbDebugging = enabled) }
        }
    }

    fun toggleOemUnlock(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setOemUnlock(enabled)
            _uiState.update { it.copy(oemUnlock = enabled) }
        }
    }

    fun toggleWifiAdb(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setWifiAdbEnabled(enabled)
            _uiState.update { it.copy(wifiAdb = enabled) }
        }
    }

    fun toggleDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            useCase.setDemoMode(enabled)
            _uiState.update { it.copy(demoMode = enabled) }
        }
    }

    fun refreshBattery() {
        viewModelScope.launch {
            val battery = useCase.getBatteryDetails()
            _uiState.update { it.copy(battery = battery) }
        }
    }

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = viewModelScope.launch {
            while (true) {
                val cpu = useCase.getCpuInfo()
                val mem = useCase.getMemoryInfo()
                val processes = useCase.getTopProcesses(15)
                _uiState.update {
                    it.copy(cpu = cpu, memory = mem, topProcesses = processes)
                }
                delay(1500)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    fun rebootDevice(serial: String) {
        viewModelScope.launch {
            useCase.rebootDevice(serial)
            refreshDevices()
        }
    }

    fun rebootToRecovery(serial: String) {
        viewModelScope.launch {
            useCase.rebootToRecovery(serial)
            refreshDevices()
        }
    }

    fun rebootToBootloader(serial: String) {
        viewModelScope.launch {
            useCase.rebootToBootloader(serial)
            refreshDevices()
        }
    }

    fun takeScreenshot(serial: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val path = useCase.takeScreenshot(serial)
            _uiState.update { it.copy(screenshotPath = path, isLoading = false) }
        }
    }

    fun clearScreenshotPath() {
        _uiState.update { it.copy(screenshotPath = null) }
    }

    fun createDiagnosticReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(reportPath = null) }
            val path = useCase.createDiagnosticReport()
            _uiState.update { it.copy(reportPath = path) }
        }
    }

    fun clearReportPath() {
        _uiState.update { it.copy(reportPath = null) }
    }
}

data class DevicesUiState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val screenshotPath: String? = null,
    val reportPath: String? = null,
    val showTouches: Boolean = false,
    val animationScale: Float = 1.0f,
    val usbDebugging: Boolean = true,
    val oemUnlock: Boolean = false,
    val wifiAdb: Boolean = false,
    val demoMode: Boolean = false,
    val battery: BatteryDetails? = null,
    val cpu: CpuInfo? = null,
    val memory: MemoryInfo? = null,
    val topProcesses: List<ProcessInfo> = emptyList()
)
