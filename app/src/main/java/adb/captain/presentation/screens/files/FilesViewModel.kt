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
            _uiState.update { it.copy(currentPath = path, isLoading = true, entries = emptyList()) }
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
            _uiState.update { it.copy(message = "Deleted: $name") }
            refresh()
        }
    }

    fun createFolder(parent: String, name: String) {
        if (name.isBlank()) return
        val path = if (parent == "/") "/$name" else "$parent/$name"
        viewModelScope.launch {
            filesUseCase.createDirectory(path)
            _uiState.update { it.copy(message = "Created: $name") }
            refresh()
        }
    }

    fun installApk(path: String) {
        viewModelScope.launch {
            val result = sideloadUseCase.installApkAtPath(path)
            _uiState.update { it.copy(message = result) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun parentOf(path: String): String? {
        if (path == "/") return null
        val trimmed = path.trimEnd('/')
        val idx = trimmed.lastIndexOf('/')
        return if (idx <= 0) "/" else trimmed.substring(0, idx)
    }
}

data class FilesUiState(
    val currentPath: String = "/sdcard",
    val entries: List<FileEntry> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)
