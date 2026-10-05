/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.logcat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.LogEntry
import adb.captain.domain.model.LogLevel
import adb.captain.domain.usecase.LogcatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogcatViewModel @Inject constructor(
    private val useCase: LogcatUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogcatUiState())
    val uiState: StateFlow<LogcatUiState> = _uiState.asStateFlow()

    /**
     * Entries currently on screen: level, app PID and text/tag query applied.
     * The export writes exactly this list, so "save" means "save what I see".
     */
    val visibleLogs: StateFlow<List<LogEntry>> = _uiState
        .map { state ->
            state.logs.filter { entry ->
                (state.selectedLevel == null || entry.level == state.selectedLevel) &&
                    (state.pidFilter == null || entry.pid == state.pidFilter) &&
                    (state.query.isBlank() ||
                        entry.message.contains(state.query, ignoreCase = true) ||
                        entry.tag.contains(state.query, ignoreCase = true))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Tap the same PID again to drop the filter. */
    fun setPidFilter(pid: Int?) {
        _uiState.update { state ->
            state.copy(pidFilter = if (state.pidFilter == pid) null else pid)
        }
    }

    /** Plain text of the filtered view, ready to be written to a file. */
    fun exportText(): String = buildExport(visibleLogs.value, _uiState.value)

    private fun startStreaming() {
        _uiState.update { it.copy(isPaused = false) }
        logcatJob = viewModelScope.launch {
            runCatching { useCase.clearLogcat() }
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
        viewModelScope.launch {
            runCatching { useCase.clearLogcat() }
            _uiState.update { it.copy(logs = emptyList()) }
        }
    }
}

data class LogcatUiState(
    val logs: List<LogEntry> = emptyList(),
    val isPaused: Boolean = true,
    val query: String = "",
    val selectedLevel: LogLevel? = null,
    /** When set, only entries from this PID (one app) are shown and exported. */
    val pidFilter: Int? = null
)

internal fun buildExport(entries: List<LogEntry>, state: LogcatUiState): String {
    val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    val filters = buildList {
        if (state.query.isNotBlank()) add("query=\"${state.query}\"")
        state.selectedLevel?.let { add("level=${it.name}") }
        state.pidFilter?.let { add("pid=$it") }
    }
    return buildString {
        appendLine("# ADB Captain log export")
        appendLine("# Exported: $stamp")
        appendLine("# Entries: ${entries.size}")
        if (filters.isNotEmpty()) appendLine("# Filters: ${filters.joinToString(" ")}")
        appendLine()
        entries.forEach { entry ->
            append(entry.timestamp)
            append(' ')
            append(entry.level.name.first())
            append('/')
            append(entry.tag)
            append(" (")
            append(entry.pid)
            append("): ")
            appendLine(entry.message)
        }
    }
}