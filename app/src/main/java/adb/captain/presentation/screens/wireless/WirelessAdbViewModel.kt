/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.wireless

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.SavedAdbDevice
import adb.captain.domain.repository.RemoteAdbRepository
import adb.captain.domain.repository.RemoteAdbState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WirelessAdbViewModel @Inject constructor(
    private val repository: RemoteAdbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WirelessAdbUiState())
    val uiState: StateFlow<WirelessAdbUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update { it.copy(connection = state) }
            }
        }
        viewModelScope.launch {
            repository.savedDevices().collect { devices ->
                _uiState.update { it.copy(savedDevices = devices) }
            }
        }
    }

    fun updateHost(value: String) = _uiState.update { it.copy(host = value) }
    fun updatePort(value: String) = _uiState.update { it.copy(port = value.filter(Char::isDigit)) }
    fun updatePairHost(value: String) = _uiState.update { it.copy(pairHost = value) }
    fun updatePairPort(value: String) = _uiState.update { it.copy(pairPort = value.filter(Char::isDigit)) }
    fun updatePairCode(value: String) = _uiState.update { it.copy(pairCode = value.filter(Char::isDigit)) }
    fun updateShellCommand(value: String) = _uiState.update { it.copy(shellCommand = value) }
    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun pair() {
        val state = _uiState.value
        val port = state.pairPort.toIntOrNull() ?: return
        if (state.pairHost.isBlank() || state.pairCode.isBlank()) return
        launchBusy {
            repository.pair(state.pairHost, port, state.pairCode)
                .onSuccess {
                    _uiState.update { it.copy(message = "Paired successfully", pairCode = "") }
                }
                .onFailure { reportError(it) }
        }
    }

    fun connect() {
        val state = _uiState.value
        val port = state.port.toIntOrNull() ?: return
        if (state.host.isBlank()) return
        launchBusy {
            repository.connect(state.host, port).onFailure { reportError(it) }
        }
    }

    fun connectSaved(device: SavedAdbDevice) {
        _uiState.update { it.copy(host = device.host, port = device.port.toString()) }
        launchBusy {
            repository.connect(device.host, device.port).onFailure { reportError(it) }
        }
    }

    fun saveCurrent(autoReconnect: Boolean) {
        val state = _uiState.value
        val port = state.port.toIntOrNull() ?: return
        if (state.host.isBlank()) return
        viewModelScope.launch {
            repository.saveDevice(
                SavedAdbDevice(
                    name = state.host,
                    host = state.host,
                    port = port,
                    autoReconnect = autoReconnect
                )
            )
            _uiState.update { it.copy(message = "Device saved") }
        }
    }

    fun deleteSaved(device: SavedAdbDevice) {
        viewModelScope.launch { repository.removeDevice(device.host, device.port) }
    }

    fun disconnect() {
        viewModelScope.launch { repository.disconnect() }
    }

    fun runShell() {
        val command = _uiState.value.shellCommand
        if (command.isBlank()) return
        launchBusy {
            repository.shell(command)
                .onSuccess { output ->
                    _uiState.update { it.copy(shellOutput = output.ifBlank { "(no output)" }) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(shellOutput = error.message ?: "Command failed") }
                }
        }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            block()
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private fun reportError(error: Throwable) {
        _uiState.update { it.copy(message = error.message ?: error::class.java.simpleName) }
    }
}

data class WirelessAdbUiState(
    val connection: RemoteAdbState = RemoteAdbState.Disconnected,
    val savedDevices: List<SavedAdbDevice> = emptyList(),
    val host: String = "",
    val port: String = "5555",
    val pairHost: String = "",
    val pairPort: String = "",
    val pairCode: String = "",
    val shellCommand: String = "getprop ro.product.model",
    val shellOutput: String = "",
    val isBusy: Boolean = false,
    val message: String? = null
)
