package adb.captain.presentation.screens.apps

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import adb.captain.domain.model.AppDetails
import adb.captain.domain.model.AppInfo
import adb.captain.domain.model.BloatwareStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val exportSaved = stringResource(R.string.apps_export_saved)
    val exportFailed = stringResource(R.string.apps_export_failed)
    val filteredApps = remember(uiState.apps, uiState.searchQuery) {
        uiState.apps.filter {
            it.packageName.contains(uiState.searchQuery, ignoreCase = true) ||
                it.label.contains(uiState.searchQuery, ignoreCase = true)
        }
    }

    val shareIntent = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let { message ->
            val text = if (message.startsWith("Error") || message.startsWith("Exception")) {
                exportFailed.format(message)
            } else {
                exportSaved.format(message)
            }
            snackbarHostState.showSnackbar(text, duration = SnackbarDuration.Long)
            viewModel.clearExportMessage()
        }
    }

    LaunchedEffect(uiState.exportCsv) {
        uiState.exportCsv?.let { content ->
            shareText(shareIntent, content, "apps.csv", "text/csv")
            viewModel.clearExportContent()
        }
    }
    LaunchedEffect(uiState.exportJson) {
        uiState.exportJson?.let { content ->
            shareText(shareIntent, content, "apps.json", "application/json")
            viewModel.clearExportContent()
        }
    }

    uiState.details?.let { details ->
        AppDetailsDialog(
            details = details,
            isLoading = uiState.isLoadingDetails,
            onDismiss = { viewModel.clearDetails() },
            onExport = { viewModel.exportApk(details.packageName) },
            onLaunch = { viewModel.launchApp(details.packageName, details.launchableActivities.firstOrNull()) }
        )
    }

    val isSelectMode = uiState.selected.isNotEmpty()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.searchApps(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.apps_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Box {
                    var exportMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { exportMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.apps_export_list))
                    }
                    DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.apps_export_csv)) },
                            onClick = { exportMenu = false; viewModel.exportListAsCsv() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.apps_export_json)) },
                            onClick = { exportMenu = false; viewModel.exportListAsJson() }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                FilterChip(
                    selected = uiState.systemFilter == null,
                    onClick = { viewModel.loadApps(null) },
                    label = { Text(stringResource(R.string.apps_filter_all)) }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = uiState.systemFilter == false,
                    onClick = { viewModel.loadApps(false) },
                    label = { Text(stringResource(R.string.apps_filter_user)) }
                )
            }

            if (isSelectMode) {
                BatchActionBar(
                    count = uiState.selected.size,
                    onForceStop = { viewModel.batchForceStop() },
                    onDisable = { viewModel.batchDisable() },
                    onEnable = { viewModel.batchEnable() },
                    onExport = { viewModel.batchExport() },
                    onClear = { viewModel.clearSelection() }
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppItem(
                            app = app,
                            exporting = uiState.isExporting,
                            isSelected = app.packageName in uiState.selected,
                            onLongClick = { viewModel.toggleSelection(app.packageName) },
                            onClick = { viewModel.loadDetails(app.packageName) },
                            onForceStop = { viewModel.forceStop(app.packageName) },
                            onClearData = { viewModel.clearData(app.packageName) },
                            onToggle = { viewModel.toggleEnabled(app.packageName, !app.enabled) },
                            onDelete = { viewModel.uninstallApp(app.packageName) },
                            onExport = { viewModel.exportApk(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatchActionBar(
    count: Int,
    onForceStop: () -> Unit,
    onDisable: () -> Unit,
    onEnable: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit
) {
    val countText = stringResource(R.string.apps_selected_count, count)
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(countText, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear) { Text(stringResource(R.string.apps_cancel)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallButton(Icons.Default.Power, stringResource(R.string.apps_force_stop), onForceStop)
                SmallButton(Icons.Default.Block, stringResource(R.string.apps_disable), onDisable)
                SmallButton(Icons.Default.CheckCircle, stringResource(R.string.apps_enable), onEnable)
                SmallButton(Icons.Default.FileDownload, stringResource(R.string.apps_export_apk), onExport)
            }
        }
    }
}

@Composable
fun SmallButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    exporting: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onForceStop: () -> Unit,
    onClearData: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.apps_more))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.apps_details)) },
                            onClick = { menuExpanded = false; onClick() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.apps_force_stop)) },
                            onClick = { menuExpanded = false; onForceStop() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.apps_clear_data)) },
                            onClick = { menuExpanded = false; onClearData() }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (app.enabled) stringResource(R.string.apps_disable)
                                    else stringResource(R.string.apps_enable)
                                )
                            },
                            onClick = { menuExpanded = false; onToggle() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.apps_delete)) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (exporting) stringResource(R.string.apps_exporting)
                                    else stringResource(R.string.apps_export_apk)
                                )
                            },
                            onClick = { menuExpanded = false; onExport() },
                            enabled = !exporting
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.apps_version, app.versionName),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                if (!app.enabled) {
                    AppStateBadge(stringResource(R.string.apps_badge_disabled), Color(0xFF9E9E9E))
                    Spacer(Modifier.width(6.dp))
                }
                BloatwareBadge(app.safetyStatus)
            }

            Text(
                stringResource(R.string.apps_tap_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AppDetailsDialog(
    details: AppDetails,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onLaunch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(details.label) },
        text = {
            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        DetailRow(stringResource(R.string.apps_details_package), details.packageName)
                    }
                    item {
                        DetailRow(stringResource(R.string.apps_details_version), "${details.versionName} (${details.versionCode})")
                    }
                    item {
                        DetailRow(stringResource(R.string.apps_details_target_sdk), details.targetSdk.toString())
                    }
                    item {
                        DetailRow(stringResource(R.string.apps_details_min_sdk), details.minSdk.toString())
                    }
                    item {
                        DetailRow(stringResource(R.string.apps_details_path), details.apkPath)
                    }
                    item {
                        DetailRow(stringResource(R.string.apps_details_data_dir), details.dataDir)
                    }
                    if (details.launchableActivities.isNotEmpty()) {
                        item {
                            DetailRow(stringResource(R.string.apps_details_launch_activity), details.launchableActivities.first())
                        }
                    }
                    item {
                        DetailRow(stringResource(R.string.apps_details_permissions, details.permissions.size), "")
                    }
                    if (details.permissions.isNotEmpty()) {
                        details.permissions.take(20).forEach { perm ->
                            item {
                                Text(perm, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(onClick = onLaunch, enabled = details.launchableActivities.isNotEmpty()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.apps_run))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onExport) {
                    Text(stringResource(R.string.apps_export_apk))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.apps_close))
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AppStateBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BloatwareBadge(status: BloatwareStatus) {
    val (textRes, color) = when (status) {
        BloatwareStatus.SAFE_TO_DELETE -> R.string.apps_safe_to_delete to Color(0xFF4CAF50)
        BloatwareStatus.NOT_SAFE_TO_DELETE -> R.string.apps_not_safe_to_delete to Color(0xFFF44336)
        BloatwareStatus.DELETE_IF_NEEDED -> R.string.apps_delete_if_needed to Color(0xFFFF9800)
        BloatwareStatus.UNKNOWN -> return
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

fun shareText(context: android.content.Context, content: String, fileName: String, mimeType: String) {
    val cachePath = java.io.File(context.cacheDir, "share")
    cachePath.mkdirs()
    val file = java.io.File(cachePath, fileName)
    file.writeText(content)
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, fileName))
}
