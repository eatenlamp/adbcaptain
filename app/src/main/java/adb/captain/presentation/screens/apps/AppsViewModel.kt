package adb.captain.presentation.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.AppDetails
import adb.captain.domain.model.AppInfo
import adb.captain.domain.usecase.AppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val useCase: AppUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps(system: Boolean? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, systemFilter = system) }
            val apps = useCase.getApps(system)
            _uiState.update { it.copy(apps = apps, isLoading = false) }
        }
    }

    fun searchApps(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun forceStop(packageName: String) {
        viewModelScope.launch { useCase.forceStop(packageName) }
    }

    fun clearData(packageName: String) {
        viewModelScope.launch { useCase.clearData(packageName) }
    }

    fun toggleEnabled(packageName: String, enable: Boolean) {
        viewModelScope.launch {
            useCase.toggle(packageName, enable)
            loadApps(_uiState.value.systemFilter)
        }
    }

    fun uninstallApp(packageName: String) {
        viewModelScope.launch {
            useCase.uninstall(packageName)
            loadApps(_uiState.value.systemFilter)
        }
    }

    fun exportApk(packageName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = useCase.exportApk(packageName)
            _uiState.update {
                it.copy(isExporting = false, exportMessage = result)
            }
        }
    }

    fun clearExportMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }

    fun loadDetails(packageName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true) }
            val details = useCase.getAppDetails(packageName)
            _uiState.update { it.copy(details = details, isLoadingDetails = false) }
        }
    }

    fun clearDetails() {
        _uiState.update { it.copy(details = null) }
    }

    fun launchApp(packageName: String, activity: String?) {
        viewModelScope.launch {
            val result = useCase.launch(packageName, activity)
            _uiState.update { it.copy(exportMessage = if (result.startsWith("Error") || result.startsWith("Exception")) result else "Launched $packageName") }
        }
    }

    fun toggleSelection(packageName: String) {
        _uiState.update {
            val sel = it.selected.toMutableSet()
            if (!sel.add(packageName)) sel.remove(packageName)
            it.copy(selected = sel)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selected = emptySet()) }
    }

    fun batchForceStop() {
        val sel = _uiState.value.selected.toList()
        viewModelScope.launch {
            sel.forEach { useCase.forceStop(it) }
            clearSelection()
        }
    }

    fun batchDisable() {
        val sel = _uiState.value.selected.toList()
        viewModelScope.launch {
            sel.forEach { useCase.toggle(it, false) }
            clearSelection()
            loadApps(_uiState.value.systemFilter)
        }
    }

    fun batchEnable() {
        val sel = _uiState.value.selected.toList()
        viewModelScope.launch {
            sel.forEach { useCase.toggle(it, true) }
            clearSelection()
            loadApps(_uiState.value.systemFilter)
        }
    }

    fun batchExport() {
        val sel = _uiState.value.selected.toList()
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val results = sel.map { it to useCase.exportApk(it) }
            val ok = results.count { !it.second.startsWith("Error") && !it.second.startsWith("Exception") }
            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportMessage = "Exported $ok/${sel.size} APKs to /sdcard/DCIM/ADBCaptain/exports",
                    selected = emptySet()
                )
            }
        }
    }

    fun exportListAsCsv() {
        viewModelScope.launch {
            val apps = _uiState.value.apps
            if (apps.isEmpty()) return@launch
            val sb = StringBuilder()
            sb.appendLine("package_name,label,version,system,enabled")
            apps.forEach {
                sb.appendLine("\"${it.packageName}\",\"${it.label}\",\"${it.versionName}\",${it.isSystem},${it.enabled}")
            }
            _uiState.update { it.copy(exportCsv = sb.toString()) }
        }
    }

    fun exportListAsJson() {
        viewModelScope.launch {
            val apps = _uiState.value.apps
            if (apps.isEmpty()) return@launch
            val sb = StringBuilder()
            sb.appendLine("[")
            apps.forEachIndexed { index, app ->
                sb.appendLine(
                    """  {"package":"${app.packageName}","label":"${app.label}","version":"${app.versionName}","system":${app.isSystem},"enabled":${app.enabled}}""".trimIndent()
                        .replace("\n", "") + if (index < apps.size - 1) "," else ""
                )
            }
            sb.appendLine("]")
            _uiState.update { it.copy(exportJson = sb.toString()) }
        }
    }

    fun clearExportContent() {
        _uiState.update { it.copy(exportCsv = null, exportJson = null) }
    }
}

data class AppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val searchQuery: String = "",
    val systemFilter: Boolean? = null,
    val exportMessage: String? = null,
    val details: AppDetails? = null,
    val isLoadingDetails: Boolean = false,
    val selected: Set<String> = emptySet(),
    val exportCsv: String? = null,
    val exportJson: String? = null
)
