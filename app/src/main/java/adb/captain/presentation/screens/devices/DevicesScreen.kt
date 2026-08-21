package adb.captain.presentation.screens.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import adb.captain.domain.model.Device
import adb.captain.domain.model.DeviceStatus
import adb.captain.domain.repository.BatteryDetails

@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRebootDialog by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val screenshotMessage = stringResource(R.string.screenshot_saved)

    LaunchedEffect(uiState.screenshotPath) {
        uiState.screenshotPath?.let { path ->
            snackbarHostState.showSnackbar(screenshotMessage.format(path))
            viewModel.clearScreenshotPath()
        }
    }
    LaunchedEffect(uiState.reportPath) {
        uiState.reportPath?.let { path ->
            showReportDialog = path.startsWith("/sdcard")
            if (!showReportDialog) {
                snackbarHostState.showSnackbar("Report failed: $path")
            }
            viewModel.clearReportPath()
        }
    }

    if (showRebootDialog != null) {
        var rebootTarget by remember { mutableStateOf("reboot") }
        AlertDialog(
            onDismissRequest = { showRebootDialog = null },
            title = { Text(stringResource(R.string.reboot_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.reboot_confirm_message))
                    Spacer(Modifier.height(12.dp))
                    Row {
                        listOf("reboot" to stringResource(R.string.device_reboot), "recovery" to stringResource(R.string.device_reboot_recovery), "bootloader" to stringResource(R.string.device_reboot_bootloader)).forEach { (key, label) ->
                            FilterChip(
                                selected = rebootTarget == key,
                                onClick = { rebootTarget = key },
                                label = { Text(label) }
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (rebootTarget) {
                            "recovery" -> viewModel.rebootToRecovery(showRebootDialog!!)
                            "bootloader" -> viewModel.rebootToBootloader(showRebootDialog!!)
                            else -> viewModel.rebootDevice(showRebootDialog!!)
                        }
                        showRebootDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.reboot_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = null }) {
                    Text(stringResource(R.string.reboot_confirm_no))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.refreshDevices() },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    ) { padding ->
        if (uiState.isLoading && uiState.devices.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.devices) { device ->
                    DeviceCard(
                        device = device,
                        onReboot = { showRebootDialog = device.serial },
                        onScreenshot = { viewModel.takeScreenshot(device.serial) }
                    )
                }

                item {
                    GlobalSwitchesSection(
                        showTouches = uiState.showTouches,
                        animationScale = uiState.animationScale,
                        usbDebugging = uiState.usbDebugging,
                        oemUnlock = uiState.oemUnlock,
                        wifiAdb = uiState.wifiAdb,
                        demoMode = uiState.demoMode,
                        onShowTouchesChange = { viewModel.toggleShowTouches(it) },
                        onAnimationScaleChange = { viewModel.setAnimationScale(it) },
                        onUsbDebuggingChange = { viewModel.toggleUsbDebugging(it) },
                        onOemUnlockChange = { viewModel.toggleOemUnlock(it) },
                        onWifiAdbChange = { viewModel.toggleWifiAdb(it) },
                        onDemoModeChange = { viewModel.toggleDemoMode(it) }
                    )
                }

                item {
                    BatteryCard(battery = uiState.battery, onRefresh = { viewModel.refreshBattery() })
                }

                item {
                    DiagnosticCard(onGenerate = { viewModel.createDiagnosticReport() })
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: Device, onReboot: () -> Unit, onScreenshot: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (device.model == "Unknown") stringResource(R.string.device_label_unknown, device.serial.take(6)) else device.model,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(device.status)
            }

            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.device_serial, device.serial), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.device_android_version, device.androidVersion, device.apiLevel), style = MaterialTheme.typography.bodySmall)

            if (device.batteryLevel >= 0) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BatteryFull,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (device.batteryLevel < 20) Color.Red else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.device_battery_level, device.batteryLevel), style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = onScreenshot,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.capture_screenshot))
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onReboot,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.device_reboot))
                }
            }
        }
    }
}

@Composable
fun GlobalSwitchesSection(
    showTouches: Boolean,
    animationScale: Float,
    usbDebugging: Boolean,
    oemUnlock: Boolean,
    wifiAdb: Boolean,
    demoMode: Boolean,
    onShowTouchesChange: (Boolean) -> Unit,
    onAnimationScaleChange: (Float) -> Unit,
    onUsbDebuggingChange: (Boolean) -> Unit,
    onOemUnlockChange: (Boolean) -> Unit,
    onWifiAdbChange: (Boolean) -> Unit,
    onDemoModeChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.global_switches_title), style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(16.dp))

            SwitchRow(
                title = stringResource(R.string.switch_show_touches),
                subtitle = stringResource(R.string.switch_show_touches_desc),
                checked = showTouches,
                onCheckedChange = onShowTouchesChange
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.switch_animation_scale), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.switch_animation_scale_desc, formatAnimationScale(animationScale)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0.0f, 0.5f, 1.0f, 1.5f, 2.0f).forEach { scale ->
                        val isSelected = animationScale == scale
                        FilterChip(
                            selected = isSelected,
                            onClick = { onAnimationScaleChange(scale) },
                            label = {
                                Text(
                                    text = formatAnimationScale(scale),
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            SwitchRow(
                title = stringResource(R.string.switch_usb_debugging),
                subtitle = stringResource(R.string.switch_usb_debugging_desc),
                checked = usbDebugging,
                onCheckedChange = onUsbDebuggingChange
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            SwitchRow(
                title = stringResource(R.string.switch_oem_unlock),
                subtitle = stringResource(R.string.switch_oem_unlock_desc),
                checked = oemUnlock,
                onCheckedChange = onOemUnlockChange
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            SwitchRow(
                title = stringResource(R.string.switch_wifi_adb),
                subtitle = stringResource(R.string.switch_wifi_adb_desc),
                checked = wifiAdb,
                onCheckedChange = onWifiAdbChange
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            SwitchRow(
                title = stringResource(R.string.switch_demo_mode),
                subtitle = stringResource(R.string.switch_demo_mode_desc),
                checked = demoMode,
                onCheckedChange = onDemoModeChange
            )
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun BatteryCard(battery: BatteryDetails?, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.battery_details_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
            }

            if (battery == null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.monitor_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                return@Column
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn(stringResource(R.string.battery_level), "${battery.level}%")
                InfoColumn(stringResource(R.string.battery_status), battery.status)
                InfoColumn(stringResource(R.string.battery_health), battery.health)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn(stringResource(R.string.battery_temperature), "${battery.temperature}°C")
                InfoColumn(stringResource(R.string.battery_voltage), "${battery.voltage} mV")
                InfoColumn(stringResource(R.string.battery_plugged), battery.plugged)
            }
            if (battery.current != 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.battery_current, battery.current),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (battery.technology.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.battery_technology, battery.technology),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun DiagnosticCard(onGenerate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.diagnostic_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.diagnostic_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGenerate) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.diagnostic_generate))
            }
        }
    }
}

@Composable
fun StatusBadge(status: DeviceStatus) {
    val color = when (status) {
        DeviceStatus.ONLINE -> Color(0xFF4CAF50)
        DeviceStatus.OFFLINE -> MaterialTheme.colorScheme.error
        DeviceStatus.UNAUTHORIZED -> Color(0xFFFF9800)
        DeviceStatus.RECOVERY -> Color(0xFF2196F3)
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun formatAnimationScale(scale: Float): String = when (scale) {
    0.0f -> "Off"
    0.5f -> "0.5x"
    1.0f -> "1x"
    1.5f -> "1.5x"
    2.0f -> "2x"
    else -> "${scale}x"
}
