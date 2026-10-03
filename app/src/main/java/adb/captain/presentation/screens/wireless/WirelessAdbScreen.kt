/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.wireless

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import adb.captain.domain.model.SavedAdbDevice
import adb.captain.domain.repository.RemoteAdbState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessAdbScreen(
    onBack: () -> Unit,
    viewModel: WirelessAdbViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.wireless_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.wireless_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectionCard(
                connection = uiState.connection,
                isBusy = uiState.isBusy,
                host = uiState.host,
                port = uiState.port,
                onHostChange = viewModel::updateHost,
                onPortChange = viewModel::updatePort,
                onConnect = viewModel::connect,
                onSave = viewModel::saveCurrent,
                onDisconnect = viewModel::disconnect
            )

            PairingCard(
                host = uiState.pairHost,
                port = uiState.pairPort,
                code = uiState.pairCode,
                isBusy = uiState.isBusy,
                onHostChange = viewModel::updatePairHost,
                onPortChange = viewModel::updatePairPort,
                onCodeChange = viewModel::updatePairCode,
                onPair = viewModel::pair
            )

            if (uiState.savedDevices.isNotEmpty()) {
                SavedDevicesCard(
                    devices = uiState.savedDevices,
                    onConnect = viewModel::connectSaved,
                    onDelete = viewModel::deleteSaved
                )
            }

            ShellCard(
                command = uiState.shellCommand,
                output = uiState.shellOutput,
                isBusy = uiState.isBusy,
                enabled = uiState.connection is RemoteAdbState.Connected,
                onCommandChange = viewModel::updateShellCommand,
                onRun = viewModel::runShell
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    connection: RemoteAdbState,
    isBusy: Boolean,
    host: String,
    port: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    onSave: (Boolean) -> Unit,
    onDisconnect: () -> Unit
) {
    var autoReconnect by remember { mutableStateOf(false) }
    val connected = connection is RemoteAdbState.Connected

    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.wireless_status_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(connection)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when (connection) {
                        is RemoteAdbState.Connected -> stringResource(
                            R.string.wireless_status_connected,
                            "${connection.host}:${connection.port}"
                        )
                        RemoteAdbState.Connecting -> stringResource(R.string.wireless_status_connecting)
                        RemoteAdbState.Disconnected -> stringResource(R.string.wireless_status_disconnected)
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isBusy) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(stringResource(R.string.wireless_add_title), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.wireless_add_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !connected,
                    label = { Text(stringResource(R.string.wireless_host)) }
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = onPortChange,
                    modifier = Modifier.width(110.dp),
                    singleLine = true,
                    enabled = !connected,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.wireless_port)) }
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.wireless_auto_reconnect), modifier = Modifier.weight(1f))
                Switch(checked = autoReconnect, onCheckedChange = { autoReconnect = it })
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (connected) {
                    Button(onClick = onDisconnect) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.wireless_disconnect))
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        enabled = host.isNotBlank() && port.isNotBlank() && !isBusy
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.wireless_connect))
                    }
                }
                OutlinedButton(
                    onClick = { onSave(autoReconnect) },
                    enabled = host.isNotBlank() && port.isNotBlank()
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.wireless_save))
                }
            }
        }
    }
}

@Composable
private fun PairingCard(
    host: String,
    port: String,
    code: String,
    isBusy: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPair: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.wireless_pair_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.wireless_pair_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.wireless_host)) }
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = onPortChange,
                    modifier = Modifier.width(110.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.wireless_port)) }
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                label = { Text(stringResource(R.string.wireless_pairing_code)) }
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onPair,
                enabled = host.isNotBlank() && port.isNotBlank() && code.isNotBlank() && !isBusy
            ) {
                Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.wireless_pair))
            }
        }
    }
}

@Composable
private fun SavedDevicesCard(
    devices: List<SavedAdbDevice>,
    onConnect: (SavedAdbDevice) -> Unit,
    onDelete: (SavedAdbDevice) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.wireless_saved_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            devices.forEach { device ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(device.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            device.endpoint + if (device.autoReconnect) " • auto" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    TextButton(onClick = { onConnect(device) }) {
                        Text(stringResource(R.string.wireless_connect))
                    }
                    IconButton(onClick = { onDelete(device) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.wireless_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShellCard(
    command: String,
    output: String,
    isBusy: Boolean,
    enabled: Boolean,
    onCommandChange: (String) -> Unit,
    onRun: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.wireless_shell_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = command,
                onValueChange = onCommandChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                label = { Text(stringResource(R.string.wireless_shell_command)) }
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRun, enabled = enabled && command.isNotBlank() && !isBusy) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.wireless_shell_run))
            }
            if (output.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.wireless_shell_output), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = output,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDot(connection: RemoteAdbState) {
    val color = when (connection) {
        is RemoteAdbState.Connected -> Color(0xFF4CAF50)
        RemoteAdbState.Connecting -> Color(0xFFFF9800)
        RemoteAdbState.Disconnected -> MaterialTheme.colorScheme.outline
    }
    Surface(color = color, shape = MaterialTheme.shapes.small, modifier = Modifier.size(12.dp)) {}
}
