/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.DebugSession
import adb.captain.domain.model.CrashReport
import adb.captain.domain.model.Macro
import adb.captain.domain.model.ProFeature
import adb.captain.domain.repository.ProRepository
import adb.captain.domain.repository.AdbRepository
import adb.captain.domain.usecase.BugPackUseCase
import adb.captain.domain.usecase.MacroUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import adb.captain.domain.model.ProLimits

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val useCase: BugPackUseCase,
    private val repository: AdbRepository,
    private val macroUseCase: MacroUseCase,
    private val proRepository: ProRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    val macros: StateFlow<List<Macro>> = macroUseCase.observe()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Whether another macro may be saved. The free build of the store is
     * limited, the FOSS build never is.
     */
    fun canSaveMacro(currentCount: Int): Boolean =
        proRepository.isUnlocked(ProFeature.UNLIMITED_MACROS) || currentCount < ProLimits.FREE_MACRO_COUNT

    fun saveMacro(name: String, commands: List<String>) {
        viewModelScope.launch {
            macroUseCase.save(name, commands)
            _uiState.update { it.copy(macroNotice = name) }
        }
    }

    fun deleteMacro(macro: Macro) {
        viewModelScope.launch { macroUseCase.delete(macro) }
    }

    fun runMacro(macro: Macro) {
        if (_uiState.value.runningMacroId == macro.id) return
        _uiState.update { it.copy(runningMacroId = macro.id, macroOutput = null) }
        viewModelScope.launch {
            val output = runCatching { macroUseCase.run(macro) }
                .getOrElse { "Error: ${it.message}" }
            _uiState.update { it.copy(runningMacroId = null, macroOutput = output) }
        }
    }

    fun loadCrashes() {
        if (_uiState.value.isLoadingCrashes) return
        _uiState.update { it.copy(isLoadingCrashes = true) }
        viewModelScope.launch {
            val crashes = runCatching { repository.getCrashReports() }.getOrDefault(emptyList())
            _uiState.update { it.copy(isLoadingCrashes = false, crashes = crashes) }
        }
    }

    private var sessionJob: Job? = null
    private val capturedLog = mutableListOf<String>()
    private val capturedPackages = linkedSetOf<String>()

    fun toggleOption(option: BugPackUseCase.Options.() -> BugPackUseCase.Options) {
        _uiState.update { state ->
            state.copy(options = option(state.options))
        }
    }

    fun setPackageName(name: String) {
        _uiState.update { it.copy(packageName = name) }
    }

    fun setNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    /** Starts watching the foreground app and buffering logcat for the report. */
    fun startSession() {
        if (_uiState.value.sessionActive) return
        capturedLog.clear()
        capturedPackages.clear()
        _uiState.update {
            it.copy(
                sessionActive = true,
                sessionStartedAt = System.currentTimeMillis(),
                sessionPackages = emptyList()
            )
        }
        sessionJob = viewModelScope.launch {
            while (isActive) {
                runCatching { repository.getForegroundApp() }.getOrNull()?.let { fg ->
                    if (fg.packageName.isNotBlank()) capturedPackages.add(fg.packageName)
                    _uiState.update { it.copy(sessionPackages = capturedPackages.toList()) }
                }
                runCatching { repository.executeCommand("logcat -d -t 150") }.getOrNull()
                    ?.lineSequence()
                    ?.forEach { capturedLog.add(it) }
                // Keep the buffer bounded so a long session cannot eat memory.
                while (capturedLog.size > MAX_LOG_LINES) capturedLog.removeAt(0)
                delay(3000)
            }
        }
    }

    fun stopSession() {
        sessionJob?.cancel()
        sessionJob = null
        _uiState.update { it.copy(sessionActive = false) }
    }

    /** Builds a bug pack; a finished session adds its own report entry. */
    fun collect(onReady: (ByteArray) -> Unit) {
        if (_uiState.value.isCollecting) return
        _uiState.update { it.copy(isCollecting = true, error = null) }
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val options = state.options.copy(packageName = state.packageName)
                val entries = useCase.collect(options).toMutableMap()
                val session = state.currentSessionOrNull(capturedLog.toList())
                if (session != null) entries["debug_session.txt"] = session.toReport()
                val zip = buildZip(entries)
                _uiState.update {
                    it.copy(isCollecting = false, entryCount = entries.size)
                }
                onReady(zip)
            } catch (e: Exception) {
                _uiState.update { it.copy(isCollecting = false, error = e.message) }
            }
        }
    }

    private fun buildZip(entries: Map<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    private companion object {
        const val MAX_LOG_LINES = 4000
    }
}

data class DebugUiState(
    val options: BugPackUseCase.Options = BugPackUseCase.Options(),
    val packageName: String = "",
    val isCollecting: Boolean = false,
    val entryCount: Int = 0,
    val error: String? = null,
    val sessionActive: Boolean = false,
    val sessionStartedAt: Long = 0L,
    val sessionPackages: List<String> = emptyList(),
    val notes: String = "",
    val runningMacroId: Long? = null,
    val macroOutput: String? = null,
    val macroNotice: String? = null,
    val isLoadingCrashes: Boolean = false,
    val crashes: List<CrashReport> = emptyList()
) {
    /** Available once a session has been started at least once. */
    fun currentSessionOrNull(logTail: List<String> = emptyList()): DebugSession? =
        if (sessionStartedAt == 0L) null else DebugSession(
            startedAt = sessionStartedAt,
            packageName = sessionPackages.lastOrNull() ?: "",
            logTail = logTail,
            notes = notes
        )
}