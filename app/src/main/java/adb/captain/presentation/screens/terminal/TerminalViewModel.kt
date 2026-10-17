/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.terminal

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.Command
import adb.captain.domain.usecase.TerminalUseCase
import adb.captain.util.AnsiParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Terminal state is split into tabs. Each tab keeps its own buffer and its own
 * running job, so switching tabs never interrupts a command in progress.
 */
@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val useCase: TerminalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private val jobs = mutableMapOf<Long, Job>()
    private var nextTabId = 1L

    val isAutoCompleteEnabled: StateFlow<Boolean> = useCase.isAutoCompleteEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val favorites: StateFlow<List<String>> = useCase.getFavoriteCommands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadHistory()
        newTab()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            useCase.getHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun newTab(title: String? = null) {
        val id = nextTabId++
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs + TerminalTab(id = id, title = title ?: defaultTabTitle(state.tabs.size)),
                activeTabId = id
            )
        }
    }

    fun selectTab(id: Long) {
        _uiState.update { it.copy(activeTabId = id) }
    }

    fun closeTab(id: Long) {
        jobs.remove(id)?.cancel()
        _uiState.update { state ->
            val remaining = state.tabs.filterNot { it.id == id }
            if (remaining.isEmpty()) {
                // Never leave the screen without a terminal.
                val fresh = nextTabId++
                state.copy(
                    tabs = listOf(TerminalTab(id = fresh, title = defaultTabTitle(0))),
                    activeTabId = fresh
                )
            } else {
                val active = if (state.activeTabId == id) remaining.first().id else state.activeTabId
                state.copy(tabs = remaining, activeTabId = active)
            }
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        val tabId = _uiState.value.activeTabId
        append(tabId, AnsiParser.parse("> $command\n"))
        renameTab(tabId, command.take(40))
        jobs[tabId]?.cancel()
        jobs[tabId] = viewModelScope.launch {
            markLoading(tabId, true)
            val result = useCase.executeCommand(command)
            append(tabId, AnsiParser.parse("${result.output}\n"))
            markLoading(tabId, false)
        }
    }

    /** Writes local text to the active tab without running a command. */
    fun printLocal(command: String, body: String) {
        if (command.isBlank()) return
        val tabId = _uiState.value.activeTabId
        append(tabId, AnsiParser.parse("> $command\n$body"))
        renameTab(tabId, command.take(40))
    }

    fun clearOutput() {
        val tabId = _uiState.value.activeTabId
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { if (it.id == tabId) it.copy(output = emptyList()) else it }
            )
        }
    }

    fun toggleFavorite(command: String) {
        viewModelScope.launch { useCase.toggleFavorite(command) }
    }

    fun clearHistory() {
        viewModelScope.launch { useCase.clearHistory() }
    }

    private fun append(tabId: Long, text: AnnotatedString) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { if (it.id == tabId) it.copy(output = it.output + text) else it }
            )
        }
    }

    private fun markLoading(tabId: Long, loading: Boolean) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { if (it.id == tabId) it.copy(isLoading = loading) else it }
            )
        }
    }

    /** First command names the tab, later commands keep that name. */
    private fun renameTab(tabId: Long, title: String) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map {
                    if (it.id == tabId && !it.hasCommandTitle) {
                        it.copy(title = title, hasCommandTitle = true)
                    } else {
                        it
                    }
                }
            )
        }
    }

    private fun defaultTabTitle(index: Int): String = "Session ${index + 1}"

    val activeTab: TerminalTab?
        get() = _uiState.value.tabs.firstOrNull { it.id == _uiState.value.activeTabId }
}

data class TerminalTab(
    val id: Long,
    val title: String,
    val output: List<AnnotatedString> = emptyList(),
    val isLoading: Boolean = false,
    /** Set once the tab took the name of its first command. */
    val hasCommandTitle: Boolean = false
)

data class TerminalUiState(
    val tabs: List<TerminalTab> = emptyList(),
    val activeTabId: Long = 0,
    val history: List<Command> = emptyList()
) {
    val activeTab: TerminalTab? get() = tabs.firstOrNull { it.id == activeTabId }
    val output: List<AnnotatedString> get() = activeTab?.output ?: emptyList()
    val isLoading: Boolean get() = activeTab?.isLoading == true
}