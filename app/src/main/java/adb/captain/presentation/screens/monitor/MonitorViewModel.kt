package adb.captain.presentation.screens.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class MonitorViewModel @Inject constructor(
    private val useCase: DeviceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonitorUiState())
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun startMonitoring() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(isRunning = true) }
                val cpu = useCase.getCpuInfo()
                val mem = useCase.getMemoryInfo()
                val processes = useCase.getTopProcesses(20)
                _uiState.update {
                    it.copy(cpu = cpu, memory = mem, topProcesses = processes, lastUpdate = System.currentTimeMillis())
                }
                delay(1500)
            }
        }
    }

    fun stopMonitoring() {
        pollingJob?.cancel()
        pollingJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    override fun onCleared() {
        stopMonitoring()
        super.onCleared()
    }
}

data class MonitorUiState(
    val isRunning: Boolean = false,
    val cpu: CpuInfo? = null,
    val memory: MemoryInfo? = null,
    val topProcesses: List<ProcessInfo> = emptyList(),
    val lastUpdate: Long = 0
)
