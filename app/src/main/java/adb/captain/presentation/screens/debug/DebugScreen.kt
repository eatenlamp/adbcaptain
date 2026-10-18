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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import adb.captain.ui.theme.ExpressiveProgress
import adb.captain.ui.theme.ExpressiveShape
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    var showAddMacro by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.debug_done)
    val failedMessage = stringResource(R.string.debug_failed)

    // Ticks once a second so the elapsed label stays live while recording.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(uiState.sessionActive) {
        while (uiState.sessionActive) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

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
            SessionCard(
                active = uiState.sessionActive,
                elapsed = uiState.sessionStartedAt
                    .takeIf { it > 0L }
                    ?.let { formatElapsed(now - it) }
                    .orEmpty(),
                apps = uiState.sessionPackages,
                onStart = viewModel::startSession,
                onStop = viewModel::stopSession
            )
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::setNotes,
                label = { Text(stringResource(R.string.debug_notes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            CrashSection(
                crashes = uiState.crashes,
                isLoading = uiState.isLoadingCrashes,
                onLoad = viewModel::loadCrashes
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.debug_macros_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showAddMacro = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.debug_macros_add))
                }
            }
            if (macros.isEmpty()) {
                Text(
                    text = stringResource(R.string.debug_macros_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            macros.forEach { macro ->
                MacroRow(
                    name = macro.name,
                    summary = macro.commands.joinToString(" ; "),
                    running = uiState.runningMacroId == macro.id,
                    onRun = { viewModel.runMacro(macro) },
                    onDelete = { viewModel.deleteMacro(macro) }
                )
            }
            uiState.macroOutput?.let { out ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = out,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
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
                    ExpressiveProgress.contained(size = 72.dp)
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
    if (showAddMacro) {
        AddMacroDialog(
            onDismiss = { showAddMacro = false },
            onSave = { name, commands ->
                viewModel.saveMacro(name, commands)
                showAddMacro = false
            }
        )
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



@Composable
private fun AddMacroDialog(
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var commands by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debug_macros_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.debug_macros_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = commands,
                    onValueChange = { commands = it },
                    label = { Text(stringResource(R.string.debug_macros_commands)) },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && commands.isNotBlank(),
                onClick = { onSave(name, commands.lines()) }
            ) { Text(stringResource(R.string.debug_macros_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun MacroRow(
    name: String,
    summary: String,
    running: Boolean,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        if (running) {
            ExpressiveProgress.indicator(size = 20.dp)
        } else {
            TextButton(onClick = onRun) { Text(stringResource(R.string.debug_macros_run)) }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = null)
        }
    }
}

@Composable
private fun SessionCard(
    active: Boolean,
    elapsed: String,
    apps: List<String>,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.debug_session_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            if (active) {
                Text(
                    text = stringResource(R.string.debug_session_running, elapsed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.debug_session_idle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (apps.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.debug_session_apps, apps.takeLast(3).joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            if (active) {
                OutlinedButton(onClick = onStop) {
                    Text(stringResource(R.string.debug_session_stop))
                }
            } else {
                Button(onClick = onStart) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.debug_session_start))
                }
            }
        }
    }
}

private fun formatElapsed(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
private fun CrashSection(
    crashes: List<adb.captain.domain.model.CrashReport>,
    isLoading: Boolean,
    onLoad: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.crash_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onLoad, enabled = !isLoading) {
                Text(
                    stringResource(
                        if (isLoading) R.string.crash_loading else R.string.crash_load
                    )
                )
            }
        }
        if (!isLoading && crashes.isEmpty()) {
            Text(
                text = stringResource(R.string.crash_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        crashes.forEach { crash ->
            CrashCard(crash)
        }
    }
}

@Composable
private fun CrashCard(crash: adb.captain.domain.model.CrashReport) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            Text(
                text = crash.packageName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = crash.timestamp + "  ·  pid " + crash.pid,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = crash.summary.lineSequence().last().trim(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = if (expanded) Int.MAX_VALUE else 2
            )
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.crash_frames, crash.stackTrace.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = crash.stackTrace.joinToString("\\n"),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
