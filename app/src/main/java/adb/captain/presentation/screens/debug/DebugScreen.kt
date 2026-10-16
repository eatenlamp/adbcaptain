/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.debug

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.debug_done)
    val failedMessage = stringResource(R.string.debug_failed)

    // Bytes collected before the user picked a destination.
    var pendingZip by remember { mutableStateOf<ByteArray?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        val bytes = pendingZip
        pendingZip = null
        if (uri == null || bytes == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                } ?: error("output stream unavailable")
            }.isSuccess
            snackbarHostState.showSnackbar(if (ok) savedMessage else failedMessage)
        }
    }

    fun collectAndSave() {
        viewModel.collect { bytes ->
            pendingZip = bytes
            val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                .format(java.util.Date())
            saveLauncher.launch("bugpack_$stamp.zip")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { collectAndSave() },
                icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                text = {
                    Text(
                        if (uiState.isCollecting)
                            stringResource(R.string.debug_collecting)
                        else
                            stringResource(R.string.debug_collect)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            OptionCheckbox(
                label = stringResource(R.string.debug_system_info),
                checked = uiState.options.systemInfo,
                onToggle = { viewModel.toggleOption { copy(systemInfo = it) } }
            )
            OptionCheckbox(
                label = stringResource(R.string.debug_package_info),
                checked = uiState.options.packageInfo,
                onToggle = { viewModel.toggleOption { copy(packageInfo = it) } }
            )
            if (uiState.options.packageInfo) {
                OutlinedTextField(
                    value = uiState.packageName,
                    onValueChange = viewModel::setPackageName,
                    label = { Text(stringResource(R.string.debug_package_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
            }
            OptionCheckbox(
                label = stringResource(R.string.debug_activity_info),
                checked = uiState.options.activityInfo,
                onToggle = { viewModel.toggleOption { copy(activityInfo = it) } }
            )
            OptionCheckbox(
                label = stringResource(R.string.debug_logcat_main),
                checked = uiState.options.logcatMain,
                onToggle = { viewModel.toggleOption { copy(logcatMain = it) } }
            )
            OptionCheckbox(
                label = stringResource(R.string.debug_logcat_events),
                checked = uiState.options.logcatEvents,
                onToggle = { viewModel.toggleOption { copy(logcatEvents = it) } }
            )
            OptionCheckbox(
                label = stringResource(R.string.debug_processes),
                checked = uiState.options.processes,
                onToggle = { viewModel.toggleOption { copy(processes = it) } }
            )
            OptionCheckbox(
                label = stringResource(R.string.debug_packages_list),
                checked = uiState.options.packagesList,
                onToggle = { viewModel.toggleOption { copy(packagesList = it) } }
            )
            OptionCheckbox(
                label = stringResource(R.string.debug_battery),
                checked = uiState.options.battery,
                onToggle = { viewModel.toggleOption { copy(battery = it) } }
            )
            OptionCheckbox(
                label = stringResource(R.string.debug_memory),
                checked = uiState.options.memory,
                onToggle = { viewModel.toggleOption { copy(memory = it) } }
            )
            OptionCheckbox(
                label = stringResource(R.string.debug_cpu),
                checked = uiState.options.cpu,
                onToggle = { viewModel.toggleOption { copy(cpu = it) } }
            )

            if (uiState.isCollecting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (uiState.entryCount > 0 && !uiState.isCollecting) {
                Text(
                    text = "${uiState.entryCount} files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OptionCheckbox(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onToggle
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
