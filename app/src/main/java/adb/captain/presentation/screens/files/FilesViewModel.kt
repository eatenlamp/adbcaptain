/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.FileEntry
import adb.captain.domain.usecase.FilesUseCase
import adb.captain.domain.usecase.SideloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val filesUseCase: FilesUseCase,
    private val sideloadUseCase: SideloadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        navigate("/sdcard")
    }

    fun navigate(path: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentPath = path,
                    isLoading = true,
                    entries = emptyList(),
                    selection = emptySet(),
                    selectionMode = false
                )
            }
            val entries = filesUseCase.listFiles(path)
            _uiState.update { it.copy(entries = entries, isLoading = false) }
        }
    }

    fun goUp() {
        val parent = parentOf(_uiState.value.currentPath)
        if (parent != null) navigate(parent)
    }

    fun refresh() = navigate(_uiState.value.currentPath)

    fun delete(path: String, name: String) {
        viewModelScope.launch {
            filesUseCase.delete(path)
            _uiState.update { it.copy(event = FilesEvent.Deleted(name)) }
            refresh()
        }
    }

    fun createFolder(parent: String, name: String) {
        if (name.isBlank()) return
        val path = joinPath(parent, name.trim())
        viewModelScope.launch {
            filesUseCase.createDirectory(path)
            _uiState.update { it.copy(event = FilesEvent.Created(name.trim())) }
            refresh()
        }
    }

    // Selection -----------------------------------------------------------------

    fun startSelection(path: String) {
        _uiState.update { it.copy(selectionMode = true, selection = setOf(path)) }
    }

    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val next = if (path in state.selection) state.selection - path else state.selection + path
            state.copy(selection = next, selectionMode = next.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selection = emptySet(), selectionMode = false) }
    }

    fun copySelection() = putSelectionOnClipboard(move = false)

    fun cutSelection() = putSelectionOnClipboard(move = true)

    fun deleteSelection() {
        val entries = _uiState.value.selectedEntries
        if (entries.isEmpty()) return
        viewModelScope.launch {
            entries.forEach { filesUseCase.delete(it.path) }
            _uiState.update {
                it.copy(
                    selection = emptySet(),
                    selectionMode = false,
                    event = FilesEvent.DeletedMany(entries.size)
                )
            }
            refresh()
        }
    }

    private fun putSelectionOnClipboard(move: Boolean) {
        val state = _uiState.value
        if (state.selection.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = state.selection.toList(),
                clipboardMove = move,
                selection = emptySet(),
                selectionMode = false,
                event = if (move) FilesEvent.Cut(state.selection.size) else FilesEvent.Copied(state.selection.size)
            )
        }
    }

    /** Copies or moves every clipboard item into the current directory. */
    fun paste() {
        val state = _uiState.value
        if (state.clipboard.isEmpty()) return
        viewModelScope.launch {
            var done = 0
            state.clipboard.forEach { source ->
                val result = if (state.clipboardMove) {
                    filesUseCase.move(source, state.currentPath)
                } else {
                    filesUseCase.copy(source, state.currentPath)
                }
                if (!result.startsWith("Error")) done++
            }
            _uiState.update {
                it.copy(
                    clipboard = emptyList(),
                    event = if (done > 0) FilesEvent.Pasted(done) else FilesEvent.Failed(null)
                )
            }
            refresh()
        }
    }

    // Rename and properties -----------------------------------------------------

    fun rename(entry: FileEntry, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty() || clean == entry.name) return
        viewModelScope.launch {
            val result = filesUseCase.rename(entry.path, clean)
            _uiState.update {
                it.copy(
                    event = if (result.startsWith("Error")) {
                        FilesEvent.Failed(result.removePrefix("Error:").trim().ifBlank { null })
                    } else {
                        FilesEvent.Renamed(clean)
                    }
                )
            }
            refresh()
        }
    }

    fun loadProperties(entry: FileEntry) {
        _uiState.update {
            it.copy(propertiesName = entry.name, propertiesText = null, propertiesLoading = true)
        }
        viewModelScope.launch {
            val text = filesUseCase.properties(entry.path)
            _uiState.update { it.copy(propertiesText = text, propertiesLoading = false) }
        }
    }

    fun dismissProperties() {
        _uiState.update { it.copy(propertiesName = null, propertiesText = null, propertiesLoading = false) }
    }

    // Transfer between the phone storage and a picker ---------------------------

    fun push(name: String, bytes: ByteArray) {
        val clean = name.substringAfterLast('/')
            .replace("\"", "_")
            .ifBlank { "file" }
        val target = joinPath(_uiState.value.currentPath, clean)
        viewModelScope.launch {
            val result = filesUseCase.push(target, bytes)
            _uiState.update {
                it.copy(
                    event = if (result.startsWith("Error")) FilesEvent.Failed(null)
                    else FilesEvent.Uploaded(clean)
                )
            }
            refresh()
        }
    }

    /** Reads a remote file and hands the bytes to the export picker. */
    fun pull(entry: FileEntry, onReady: (ByteArray) -> Unit) {
        viewModelScope.launch {
            val bytes = filesUseCase.pull(entry.path)
            if (bytes == null) {
                _uiState.update { it.copy(event = FilesEvent.Failed(null)) }
            } else {
                onReady(bytes)
            }
        }
    }

    fun installApk(path: String) {
        viewModelScope.launch {
            val result = sideloadUseCase.installApkAtPath(path)
            _uiState.update { it.copy(event = FilesEvent.Installed(result)) }
        }
    }

    fun clearEvent() {
        _uiState.update { it.copy(event = null) }
    }

    private fun joinPath(dir: String, name: String): String =
        if (dir == "/") "/$name" else "${dir.trimEnd('/')}/$name"

    private fun parentOf(path: String): String? {
        if (path == "/") return null
        val trimmed = path.trimEnd('/')
        val idx = trimmed.lastIndexOf('/')
        return if (idx <= 0) "/" else trimmed.substring(0, idx)
    }
}

/** One-off feedback for the snackbar; the screen owns the wording. */
sealed interface FilesEvent {
    data class Deleted(val name: String) : FilesEvent
    data class DeletedMany(val count: Int) : FilesEvent
    data class Created(val name: String) : FilesEvent
    data class Renamed(val name: String) : FilesEvent
    data class Copied(val count: Int) : FilesEvent
    data class Cut(val count: Int) : FilesEvent
    data class Pasted(val count: Int) : FilesEvent
    data class Uploaded(val name: String) : FilesEvent
    data class Installed(val result: String) : FilesEvent
    data class Failed(val details: String?) : FilesEvent
}

data class FilesUiState(
    val currentPath: String = "/sdcard",
    val entries: List<FileEntry> = emptyList(),
    val isLoading: Boolean = false,
    val event: FilesEvent? = null,
    val selection: Set<String> = emptySet(),
    val selectionMode: Boolean = false,
    val clipboard: List<String> = emptyList(),
    val clipboardMove: Boolean = false,
    val propertiesName: String? = null,
    val propertiesText: String? = null,
    val propertiesLoading: Boolean = false
) {
    val selectedEntries: List<FileEntry> get() = entries.filter { it.path in selection }
}
