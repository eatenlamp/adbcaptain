package adb.captain.presentation.screens.devices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import adb.captain.domain.model.Device
import adb.captain.domain.model.DeviceStatus
import adb.captain.domain.repository.BatteryDetails
import adb.captain.domain.repository.BatteryHealth
import adb.captain.domain.repository.BatteryPowerSource
import adb.captain.domain.repository.BatteryStatus

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
                        nfcEnabled = uiState.nfcEnabled,
                        mobileDataEnabled = uiState.mobileDataEnabled,
                        onShowTouchesChange = { viewModel.toggleShowTouches(it) },
                        onAnimationScaleChange = { viewModel.setAnimationScale(it) },
                        onUsbDebuggingChange = { viewModel.toggleUsbDebugging(it) },
                        onOemUnlockChange = { viewModel.toggleOemUnlock(it) },
                        onWifiAdbChange = { viewModel.toggleWifiAdb(it) },
                        onDemoModeChange = { viewModel.toggleDemoMode(it) },
                        onNfcChange = { viewModel.toggleNfc(it) },
                        onMobileDataChange = { viewModel.toggleMobileData(it) }
                    )
                }

                item {
                    DisplayCard(
                        nightMode = uiState.nightMode,
                        displayDensity = uiState.displayDensity,
                        displaySize = uiState.displaySize,
                        onNightModeChange = { viewModel.setNightMode(it) },
                        onBrightnessSet = { viewModel.setScreenBrightness(it) },
                        onDensityApply = { viewModel.applyDisplayDensity(it) },
                        onSizeApply = { w, h -> viewModel.applyDisplaySize(w, h) },
                        onDisplayReset = { viewModel.resetDisplay() }
                    )
                }

                item {
                    BatteryCard(
                        battery = uiState.battery,
                        onRefresh = { viewModel.refreshBattery() },
                        onSetLevel = { viewModel.setBatteryLevel(it) },
                        onReset = { viewModel.resetBattery() }
                    )
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
    nfcEnabled: Boolean,
    mobileDataEnabled: Boolean,
    onShowTouchesChange: (Boolean) -> Unit,
    onAnimationScaleChange: (Float) -> Unit,
    onUsbDebuggingChange: (Boolean) -> Unit,
    onOemUnlockChange: (Boolean) -> Unit,
    onWifiAdbChange: (Boolean) -> Unit,
    onDemoModeChange: (Boolean) -> Unit,
    onNfcChange: (Boolean) -> Unit,
    onMobileDataChange: (Boolean) -> Unit
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
                title = stringResource(R.string.switch_nfc),
                subtitle = stringResource(R.string.switch_nfc_desc),
                checked = nfcEnabled,
                onCheckedChange = onNfcChange
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            SwitchRow(
                title = stringResource(R.string.switch_mobile_data),
                subtitle = stringResource(R.string.switch_mobile_data_desc),
                checked = mobileDataEnabled,
                onCheckedChange = onMobileDataChange
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
fun DisplayCard(
    nightMode: Int,
    displayDensity: Int,
    displaySize: String?,
    onNightModeChange: (Int) -> Unit,
    onBrightnessSet: (Int) -> Unit,
    onDensityApply: (Int) -> Unit,
    onSizeApply: (Int, Int) -> Unit,
    onDisplayReset: () -> Unit
) {
    var densityInput by remember(displayDensity) { mutableStateOf(if (displayDensity > 0) displayDensity.toString() else "") }
    val sizeParts = displaySize?.split("x")
    var widthInput by remember(sizeParts) { mutableStateOf(sizeParts?.getOrNull(0) ?: "") }
    var heightInput by remember(sizeParts) { mutableStateOf(sizeParts?.getOrNull(1) ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.display_title), style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(12.dp))

            Text(stringResource(R.string.night_mode_title), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = nightMode == 0, onClick = { onNightModeChange(0) }, label = { Text(stringResource(R.string.night_mode_auto)) })
                FilterChip(selected = nightMode == 2, onClick = { onNightModeChange(2) }, label = { Text(stringResource(R.string.night_mode_on)) })
                FilterChip(selected = nightMode == 1, onClick = { onNightModeChange(1) }, label = { Text(stringResource(R.string.night_mode_off)) })
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(stringResource(R.string.brightness_title), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 25, 50, 100).forEach { level ->
                    AssistChip(onClick = { onBrightnessSet(level) }, label = { Text("$level%") })
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(stringResource(R.string.display_density_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.display_density_current, displayDensity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = densityInput,
                    onValueChange = { densityInput = it },
                    modifier = Modifier.width(110.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    label = { Text(stringResource(R.string.display_density_label)) }
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { densityInput.toIntOrNull()?.let(onDensityApply) },
                    enabled = densityInput.toIntOrNull() != null
                ) {
                    Text(stringResource(R.string.display_apply))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(stringResource(R.string.display_size_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.display_size_current, displaySize ?: "—"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = widthInput,
                    onValueChange = { widthInput = it },
                    modifier = Modifier.width(90.dp),
                    singleLine = true,
                    label = { Text("W") }
                )
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    modifier = Modifier.width(90.dp),
                    singleLine = true,
                    label = { Text("H") }
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val w = widthInput.toIntOrNull()
                        val h = heightInput.toIntOrNull()
                        if (w != null && h != null) onSizeApply(w, h)
                    },
                    enabled = widthInput.toIntOrNull() != null && heightInput.toIntOrNull() != null
                ) {
                    Text(stringResource(R.string.display_apply))
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDisplayReset) {
                Text(stringResource(R.string.display_reset))
            }
        }
    }
}

@Composable
fun BatteryCard(
    battery: BatteryDetails?,
    onRefresh: () -> Unit,
    onSetLevel: (Int) -> Unit,
    onReset: () -> Unit
) {
    var showLevelDialog by remember { mutableStateOf(false) }

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

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                BatteryRing(
                    level = battery.level,
                    charging = battery.status == BatteryStatus.CHARGING ||
                        battery.status == BatteryStatus.FULL
                )
                Spacer(Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BatteryStatusPill(battery.status)
                    BatteryHealthPill(battery.health)
                    BatteryPowerRow(battery)
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn(
                    stringResource(R.string.battery_temperature),
                    "${battery.temperature}°C"
                )
                InfoColumn(
                    stringResource(R.string.battery_voltage),
                    "${battery.voltage} mV"
                )
                InfoColumn(
                    stringResource(R.string.battery_current_label),
                    if (battery.current != 0) "%+d mA".format(battery.current) else "—"
                )
            }

            if (battery.capacity > 0 || battery.chargeCounter != null) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (battery.capacity > 0) {
                        InfoColumn(
                            stringResource(R.string.battery_capacity),
                            "${battery.capacity} mAh"
                        )
                    }
                    battery.chargeCounter?.let { counter ->
                        if (counter != 0) {
                            InfoColumn(
                                stringResource(R.string.battery_charge_counter),
                                "$counter µAh"
                            )
                        }
                    }
                    if (battery.technology.isNotBlank()) {
                        InfoColumn(
                            stringResource(R.string.battery_technology_label),
                            battery.technology
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { showLevelDialog = true }) {
                    Icon(Icons.Default.BatteryStd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.battery_set_level))
                }
                OutlinedButton(onClick = onReset) {
                    Text(stringResource(R.string.battery_reset))
                }
            }
        }
    }

    if (showLevelDialog) {
        var levelInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLevelDialog = false },
            title = { Text(stringResource(R.string.battery_set_level)) },
            text = {
                OutlinedTextField(
                    value = levelInput,
                    onValueChange = { levelInput = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.battery_level)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        levelInput.toIntOrNull()?.let(onSetLevel)
                        showLevelDialog = false
                    },
                    enabled = levelInput.toIntOrNull() != null
                ) {
                    Text(stringResource(R.string.display_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLevelDialog = false }) {
                    Text(stringResource(R.string.apps_cancel))
                }
            }
        )
    }
}

@Composable
private fun BatteryRing(level: Int, charging: Boolean) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progress = when {
        level >= 50 -> Color(0xFF4CAF50)
        level >= 20 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.error
    }
    Box(modifier = Modifier.size(116.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - strokeWidth,
                size.height - strokeWidth
            )
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = progress,
                startAngle = 135f,
                sweepAngle = 270f * (level.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$level%",
                style = MaterialTheme.typography.headlineMedium,
                color = progress
            )
            if (charging) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = progress
                )
            }
        }
    }
}

@Composable
private fun BatteryPill(labelRes: Int, color: Color, icon: ImageVector) {
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun BatteryStatusPill(status: BatteryStatus) {
    when (status) {
        BatteryStatus.CHARGING -> BatteryPill(
            R.string.battery_charging,
            Color(0xFF4CAF50),
            Icons.Default.Bolt
        )
        BatteryStatus.DISCHARGING -> BatteryPill(
            R.string.battery_discharging,
            Color(0xFF2196F3),
            Icons.Default.BatteryStd
        )
        BatteryStatus.FULL -> BatteryPill(
            R.string.battery_full,
            Color(0xFF4CAF50),
            Icons.Default.BatteryFull
        )
        BatteryStatus.NOT_CHARGING -> BatteryPill(
            R.string.battery_not_charging,
            Color(0xFFFF9800),
            Icons.Default.BatteryStd
        )
        BatteryStatus.UNKNOWN -> BatteryPill(
            R.string.battery_status_unknown,
            MaterialTheme.colorScheme.outline,
            Icons.Default.BatteryUnknown
        )
    }
}

@Composable
private fun BatteryHealthPill(health: BatteryHealth) {
    when (health) {
        BatteryHealth.GOOD -> BatteryPill(
            R.string.battery_health_good,
            Color(0xFF4CAF50),
            Icons.Default.Verified
        )
        BatteryHealth.OVERHEAT -> BatteryPill(
            R.string.battery_health_overheat,
            MaterialTheme.colorScheme.error,
            Icons.Default.Warning
        )
        BatteryHealth.DEAD -> BatteryPill(
            R.string.battery_health_dead,
            MaterialTheme.colorScheme.error,
            Icons.Default.Error
        )
        BatteryHealth.OVER_VOLTAGE -> BatteryPill(
            R.string.battery_health_overvoltage,
            MaterialTheme.colorScheme.error,
            Icons.Default.Warning
        )
        BatteryHealth.FAILURE -> BatteryPill(
            R.string.battery_health_failure,
            MaterialTheme.colorScheme.error,
            Icons.Default.Error
        )
        BatteryHealth.COLD -> BatteryPill(
            R.string.battery_health_cold,
            Color(0xFF2196F3),
            Icons.Default.AcUnit
        )
        BatteryHealth.UNKNOWN -> BatteryPill(
            R.string.battery_health_unknown,
            MaterialTheme.colorScheme.outline,
            Icons.Default.HelpOutline
        )
    }
}

@Composable
private fun BatteryPowerRow(battery: BatteryDetails) {
    val (labelRes, icon) = when (battery.plugged) {
        BatteryPowerSource.AC -> R.string.battery_power_ac to Icons.Default.Power
        BatteryPowerSource.USB -> R.string.battery_power_usb to Icons.Default.Usb
        BatteryPowerSource.WIRELESS -> R.string.battery_power_wireless to Icons.Default.WifiTethering
        BatteryPowerSource.DOCK -> R.string.battery_power_dock to Icons.Default.Dock
        BatteryPowerSource.NONE -> R.string.battery_power_none to Icons.Default.BatteryStd
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
