/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.monitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import adb.captain.domain.repository.CpuInfo
import adb.captain.domain.repository.MemoryInfo
import adb.captain.domain.repository.ProcessInfo
import java.text.DecimalFormat

@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showProcesses by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (showProcesses) {
        ProcessListDialog(
            processes = uiState.allProcesses,
            isLoading = uiState.isLoadingProcesses,
            failed = uiState.processesError,
            onReload = viewModel::loadAllProcesses,
            onStop = viewModel::stopProcess,
            onDismiss = { showProcesses = false; viewModel.clearProcesses() }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.isRunning) viewModel.stopMonitoring()
                    else viewModel.startMonitoring()
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    if (uiState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CpuCard(uiState.cpu)
            }
            item {
                MemoryCard(uiState.memory)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.monitor_top_processes),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showProcesses = true; viewModel.loadAllProcesses() }) {
                        Text(stringResource(R.string.pm_open))
                    }
                }
            }
            if (uiState.topProcesses.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.monitor_no_processes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                items(uiState.topProcesses) { process ->
                    ProcessRow(process)
                }
            }
        }
    }
}

@Composable
fun CpuCard(cpu: CpuInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.monitor_cpu), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                if (cpu != null) {
                    Text(
                        stringResource(R.string.monitor_percent, cpu.totalUsage),
                        style = MaterialTheme.typography.titleLarge,
                        color = cpuColor(cpu.totalUsage)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (cpu?.totalUsage ?: 0f) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = cpuColor(cpu?.totalUsage ?: 0f)
            )
            if (cpu != null && cpu.perCoreUsage.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.monitor_per_core, cpu.perCoreUsage.joinToString(" ") { it.toString() }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (cpu != null && cpu.frequency.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.monitor_frequency, cpu.frequency.joinToString(" ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (cpu == null) {
                Text(
                    stringResource(R.string.monitor_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun MemoryCard(memory: MemoryInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.monitor_memory), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                if (memory != null) {
                    Text(
                        stringResource(R.string.monitor_percent, memoryUsagePercent(memory)),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (memory != null) {
                LinearProgressIndicator(
                    progress = { memoryUsagePercent(memory) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.monitor_mem_used, formatBytes(memory.usedMem)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(R.string.monitor_mem_total, formatBytes(memory.totalMem)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.monitor_mem_available, formatBytes(memory.availableMem)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (memory.swapTotal > 0) {
                        Text(
                            stringResource(R.string.monitor_mem_swap, formatBytes(memory.swapFree), formatBytes(memory.swapTotal)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    stringResource(R.string.monitor_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ProcessRow(process: ProcessInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    process.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    stringResource(R.string.monitor_pid, process.pid),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.monitor_percent, process.cpuUsage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cpuColor(process.cpuUsage)
                )
                Text(
                    formatBytes(process.memoryUsage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun memoryUsagePercent(memory: MemoryInfo): Float {
    if (memory.totalMem <= 0) return 0f
    return (memory.usedMem.toFloat() / memory.totalMem * 100).coerceIn(0f, 100f)
}

private fun cpuColor(usage: Float): Color = when {
    usage >= 80 -> Color(0xFFF44336)
    usage >= 50 -> Color(0xFFFF9800)
    else -> Color(0xFF4CAF50)
}

private val decimalFormat = DecimalFormat("#.#")

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${decimalFormat.format(kb)} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${decimalFormat.format(mb)} MB"
    val gb = mb / 1024.0
    return "${decimalFormat.format(gb)} GB"
}

@Composable
private fun ProcessListDialog(
    processes: List<ProcessInfo>,
    isLoading: Boolean,
    failed: Boolean,
    onReload: () -> Unit,
    onStop: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(processes, query) {
        if (query.isBlank()) processes else processes.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.pid.toString().contains(query) ||
                it.user.contains(query, ignoreCase = true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pm_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.pm_search)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                } else if (failed) {
                    Text(stringResource(R.string.pm_load_error))
                } else {
                    Text(
                        stringResource(R.string.pm_count, filtered.size),
                        style = MaterialTheme.typography.labelMedium
                    )
                    LazyColumn(modifier = Modifier.height(360.dp)) {
                        items(filtered) { p -> ProcessDetailRow(p, onStop) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pm_close)) } },
        dismissButton = { TextButton(onClick = onReload) { Text(stringResource(R.string.pm_open)) } }
    )
}

@Composable
private fun ProcessDetailRow(process: ProcessInfo, onStop: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(process.name.ifBlank { process.cmdline }, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.pm_pid, process.pid, process.ppid) +
                    "  " + process.user,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.pm_memory,
                    formatBytes(process.memoryUsage),
                    formatBytes(process.virtualMemory)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        process.stoppablePackage?.let { pkg ->
            TextButton(onClick = { onStop(pkg) }) {
                Text(stringResource(R.string.pm_stop), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
