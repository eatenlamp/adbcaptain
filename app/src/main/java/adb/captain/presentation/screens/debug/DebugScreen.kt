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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    initialMacroId: Long? = null,
    onInitialMacroHandled: () -> Unit = {},
    viewModel: DebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    var editingMacro by remember { mutableStateOf<adb.captain.domain.model.Macro?>(null) }
    var variablesMacro by remember { mutableStateOf<adb.captain.domain.model.Macro?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.debug_done)
    val failedMessage = stringResource(R.string.debug_failed)
    val exportFailedMessage = stringResource(R.string.debug_macros_export_failed)
    val importFailedMessage = stringResource(R.string.debug_macros_import_failed)
    val exportedMessage = stringResource(R.string.debug_macros_exported)
    val importedMessage = stringResource(R.string.debug_macros_imported)

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

    // Macro export: the JSON is built first, then handed to the file picker.
    var pendingMacroJson by remember { mutableStateOf<String?>(null) }
    val macroExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val json = pendingMacroJson
        pendingMacroJson = null
        if (uri == null || json == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray())
                } ?: error("output stream unavailable")
            }.isSuccess
            snackbarHostState.showSnackbar(if (ok) exportedMessage else exportFailedMessage)
        }
    }

    val macroImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
            }.getOrNull()
            val ok = text != null && viewModel.importMacro(text)
            snackbarHostState.showSnackbar(if (ok) importedMessage else importFailedMessage)
        }
    }

    LaunchedEffect(uiState.macroNotice) {
        val notice = uiState.macroNotice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(notice)
        viewModel.clearMacroNotice()
    }

    // Opened from a home-screen shortcut: run that macro right away.
    LaunchedEffect(initialMacroId) {
        val id = initialMacroId ?: return@LaunchedEffect
        val macro = macros.firstOrNull { it.id == id } ?: viewModel.findMacro(id)
        if (macro != null) {
            if (macro.variables.isEmpty()) viewModel.runMacro(macro) else variablesMacro = macro
        }
        onInitialMacroHandled()
    }

    fun exportMacro(macro: adb.captain.domain.model.Macro) {
        pendingMacroJson = viewModel.macroJson(macro)
        val safeName = macro.name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "macro" }
        macroExportLauncher.launch("$safeName.json")
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
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.debug_macros_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { macroImportLauncher.launch(arrayOf("application/json", "text/plain")) }) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = stringResource(R.string.debug_macros_import)
                    )
                }
                TextButton(
                    onClick = {
                        editingMacro = null
                        showEditor = true
                    }
                ) {
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
                    macro = macro,
                    running = uiState.runningMacroId == macro.id,
                    onRun = {
                        if (macro.variables.isEmpty()) {
                            viewModel.runMacro(macro)
                        } else {
                            variablesMacro = macro
                        }
                    },
                    onEdit = {
                        editingMacro = macro
                        showEditor = true
                    },
                    onExport = { exportMacro(macro) },
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
    if (showEditor) {
        MacroEditorDialog(
            initial = editingMacro,
            onDismiss = {
                showEditor = false
                editingMacro = null
            },
            onSave = { name, steps, id ->
                viewModel.saveMacro(name, steps, id)
                showEditor = false
                editingMacro = null
            }
        )
    }
    variablesMacro?.let { macro ->
        MacroVariablesDialog(
            macro = macro,
            onDismiss = { variablesMacro = null },
            onRun = { values ->
                variablesMacro = null
                viewModel.runMacro(macro, values)
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
