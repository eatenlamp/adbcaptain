package adb.captain.presentation.screens.logcat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.LogEntry
import adb.captain.domain.model.LogLevel
import adb.captain.domain.usecase.LogcatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogcatViewModel @Inject constructor(
    private val useCase: LogcatUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogcatUiState())
    val uiState: StateFlow<LogcatUiState> = _uiState.asStateFlow()

    private var logcatJob: Job? = null
    private var nextEntryId = 0L

    fun toggleStream() {
        if (_uiState.value.isPaused) {
            startStreaming()
        } else {
            pauseStreaming()
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun setLevel(level: LogLevel?) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    private fun startStreaming() {
        _uiState.update { it.copy(isPaused = false) }
        logcatJob = viewModelScope.launch {
            useCase.streamLogcat().collect { entry ->
                val withId = entry.copy(id = nextEntryId++)
                _uiState.update { state ->
                    state.copy(logs = (state.logs + withId).takeLast(3000))
                }
            }
        }
    }

    private fun pauseStreaming() {
        logcatJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }
}

data class LogcatUiState(
    val logs: List<LogEntry> = emptyList(),
    val isPaused: Boolean = true,
    val query: String = "",
    val selectedLevel: LogLevel? = null
)
