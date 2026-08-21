package adb.captain.presentation.screens.sideload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import adb.captain.ui.theme.JetBrainsMono
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R

@Composable
fun SideloadScreen(
    viewModel: SideloadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = stringResource(R.string.sideload_success)
    val failedMessage = stringResource(R.string.sideload_failed)
    val screenshotMessagePrefix = stringResource(R.string.screenshot_saved)
    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var overlayEnabled by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            viewModel.startOverlay()
            overlayEnabled = true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        overlayEnabled = checkOverlayPermissionAndStart(context, viewModel, overlayLauncher)
    }

    val apkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.installApk(it) }
    }

    // Show result messages
    LaunchedEffect(uiState.installResult) {
        uiState.installResult?.let { result ->
            val isSuccess = result.startsWith("Success") || result.contains("Success")
            snackbarHostState.showSnackbar(
                message = if (isSuccess) successMessage else "$failedMessage\n$result"
            )
            viewModel.clearInstallResult()
        }
    }
    LaunchedEffect(uiState.screenshotPath) {
        uiState.screenshotPath?.let { path ->
            snackbarHostState.showSnackbar(screenshotMessagePrefix.format(path))
            viewModel.clearScreenshotPath()
        }
    }

    Scaffold(
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
            // APK Sideload
            SectionCard(
                title = stringResource(R.string.sideload_apk),
                subtitle = stringResource(R.string.sideload_apk_subtitle)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { apkLauncher.launch("application/vnd.android.package-archive") },
                        enabled = !uiState.isInstalling
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isInstalling) stringResource(R.string.sideload_installing) else stringResource(R.string.sideload_pick_apk))
                    }
                    if (uiState.isInstalling) {
                        Spacer(Modifier.width(12.dp))
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }

            // Capture
            SectionCard(
                title = stringResource(R.string.capture),
                subtitle = null
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        icon = Icons.Default.PhotoCamera,
                        label = stringResource(R.string.capture_screenshot),
                        onClick = { viewModel.takeScreenshot() }
                    )
                    ActionButton(
                        icon = Icons.Default.Videocam,
                        label = if (uiState.isRecording) {
                            stringResource(R.string.record_stop) + " (${formatDuration(uiState.recordingSeconds)})"
                        } else {
                            stringResource(R.string.record_start)
                        },
                        onClick = { viewModel.toggleRecording() },
                        emphasized = uiState.isRecording
                    )
                }
            }

            // Device Controls
            SectionCard(
                title = stringResource(R.string.device_controls),
                subtitle = null
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(icon = Icons.Default.Power, label = stringResource(R.string.device_wake), onClick = { viewModel.wakeDevice() })
                    ActionButton(icon = Icons.Default.LockOpen, label = stringResource(R.string.device_unlock), onClick = { viewModel.unlockDevice() })
                }
                Spacer(Modifier.height(4.dp))
                ToggleRow(
                    icon = Icons.Default.Power,
                    label = stringResource(R.string.device_stay_awake),
                    checked = uiState.stayAwake,
                    onCheckedChange = { viewModel.setStayAwake(it) }
                )
                ToggleRow(
                    icon = Icons.Default.Wifi,
                    label = stringResource(R.string.device_wifi),
                    checked = uiState.wifiEnabled,
                    onCheckedChange = { viewModel.setWifiEnabled(it) }
                )
                ToggleRow(
                    icon = Icons.Default.Bluetooth,
                    label = stringResource(R.string.device_bluetooth),
                    checked = uiState.bluetoothEnabled,
                    onCheckedChange = { viewModel.setBluetoothEnabled(it) }
                )
                ToggleRow(
                    icon = Icons.Filled.AirplanemodeActive,
                    label = stringResource(R.string.device_airplane),
                    checked = uiState.airplaneMode,
                    onCheckedChange = { viewModel.setAirplaneMode(it) }
                )
                ToggleRow(
                    icon = Icons.Default.PictureInPictureAlt,
                    label = stringResource(R.string.overlay_toggle),
                    checked = overlayEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            // On Android 13+ a foreground service needs notification permission
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                overlayEnabled = checkOverlayPermissionAndStart(context, viewModel, overlayLauncher)
                            }
                        } else {
                            viewModel.stopOverlay()
                            overlayEnabled = false
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.device_volume), style = MaterialTheme.typography.bodyMedium)
                }
                var volume by remember { mutableStateOf(uiState.volumeLevel.toFloat()) }
                LaunchedEffect(uiState.volumeLevel) { volume = uiState.volumeLevel.toFloat() }
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    onValueChangeFinished = { viewModel.setMediaVolume(volume.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Input & Links
            SectionCard(
                title = stringResource(R.string.input_links),
                subtitle = null
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.input_text)) },
                    leadingIcon = { Icon(Icons.Default.Keyboard, contentDescription = null) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.sendText(textInput)
                        textInput = ""
                    },
                    enabled = textInput.isNotBlank()
                ) {
                    Text(stringResource(R.string.input_send))
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.input_url)) },
                    placeholder = { Text(stringResource(R.string.input_url_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    textStyle = LocalTextStyle.current.copy(fontFamily = JetBrainsMono)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.openUrl(urlInput)
                        urlInput = ""
                    },
                    enabled = urlInput.isNotBlank()
                ) {
                    Text(stringResource(R.string.input_url))
                }
            }

            // Macros
            MacrosSection(
                isRunning = uiState.isMacroRunning,
                result = uiState.macroResult,
                onRun = { viewModel.runMacro(it) },
                onClearResult = { viewModel.clearMacroResult() }
            )
        }
    }
}

@Composable
fun MacrosSection(
    isRunning: Boolean,
    result: String?,
    onRun: (String) -> Unit,
    onClearResult: () -> Unit
) {
    var script by remember { mutableStateOf(DEFAULT_MACRO) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(result) {
        result?.let {
            snackbarHostState.showSnackbar(it)
            onClearResult()
        }
    }

    SectionCard(
        title = stringResource(R.string.macros_title),
        subtitle = stringResource(R.string.macros_desc)
    ) {
        OutlinedTextField(
            value = script,
            onValueChange = { script = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.macros_script)) },
            minLines = 8,
            textStyle = LocalTextStyle.current.copy(fontFamily = JetBrainsMono, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onRun(script) },
            enabled = !isRunning && script.isNotBlank()
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.macros_running))
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.macros_run))
            }
        }
    }
}

private const val DEFAULT_MACRO = """delay 1000
input tap 540 900
delay 500
input keyevent KEYCODE_HOME
delay 800
input swipe 100 1800 900 1800 300"""

@Composable
fun SectionCard(title: String, subtitle: String?, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit, emphasized: Boolean = false) {
    FilledTonalButton(
        onClick = onClick,
        colors = if (emphasized) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        }
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun checkOverlayPermissionAndStart(
    context: android.content.Context,
    viewModel: SideloadViewModel,
    overlayLauncher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>
): Boolean {
    if (Settings.canDrawOverlays(context)) {
        viewModel.startOverlay()
        return true
    }
    overlayLauncher.launch(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    )
    return false
}
