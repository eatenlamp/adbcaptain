package adb.captain.presentation.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.Command
import adb.captain.domain.usecase.TerminalUseCase
import adb.captain.util.AnsiParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val useCase: TerminalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    val isAutoCompleteEnabled: StateFlow<Boolean> = useCase.isAutoCompleteEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            useCase.getHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = useCase.executeCommand(command)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    output = state.output + AnsiParser.parse("> $command\n${result.output}")
                )
            }
        }
    }

    /**
     * Выводит локальный текст в терминал без выполнения и без сохранения в историю (например, help).
     */
    fun printLocal(command: String, body: String) {
        if (command.isBlank()) return
        _uiState.update { state ->
            state.copy(output = state.output + AnsiParser.parse("> $command\n$body"))
        }
    }

    fun clearOutput() {
        _uiState.update { it.copy(output = emptyList()) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            useCase.clearHistory()
        }
    }
}

data class TerminalUiState(
    val output: List<androidx.compose.ui.text.AnnotatedString> = emptyList(),
    val history: List<Command> = emptyList(),
    val isLoading: Boolean = false
)
