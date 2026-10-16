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
import adb.captain.domain.usecase.BugPackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val useCase: BugPackUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    fun toggleOption(option: BugPackUseCase.Options.() -> BugPackUseCase.Options) {
        _uiState.update { state ->
            state.copy(options = option(state.options))
        }
    }

    fun setPackageName(name: String) {
        _uiState.update { it.copy(packageName = name) }
    }

    fun collect(onReady: (ByteArray) -> Unit) {
        if (_uiState.value.isCollecting) return
        _uiState.update { it.copy(isCollecting = true, error = null) }
        viewModelScope.launch {
            try {
                val options = _uiState.value.options.copy(
                    packageName = _uiState.value.packageName
                )
                val entries = useCase.collect(options)
                val zip = buildZip(entries)
                _uiState.update { it.copy(isCollecting = false, entryCount = entries.size) }
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
}

data class DebugUiState(
    val options: BugPackUseCase.Options = BugPackUseCase.Options(),
    val packageName: String = "",
    val isCollecting: Boolean = false,
    val entryCount: Int = 0,
    val error: String? = null
)
