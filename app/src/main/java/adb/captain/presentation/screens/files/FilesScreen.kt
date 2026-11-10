/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import adb.captain.ui.theme.ExpressiveProgress
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import adb.captain.domain.model.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: FilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val failedMessage = stringResource(R.string.files_operation_failed)

    LaunchedEffect(uiState.event) {
        val event = uiState.event ?: return@LaunchedEffect
        val text = when (event) {
            is FilesEvent.Deleted -> context.getString(R.string.files_deleted, event.name)
            is FilesEvent.DeletedMany -> context.getString(R.string.files_deleted_many, event.count)
            is FilesEvent.Created -> context.getString(R.string.files_created, event.name)
            is FilesEvent.Renamed -> context.getString(R.string.files_renamed, event.name)
            is FilesEvent.Copied -> context.getString(R.string.files_copied, event.count)
            is FilesEvent.Cut -> context.getString(R.string.files_cut_done, event.count)
            is FilesEvent.Pasted -> context.getString(R.string.files_pasted, event.count)
            is FilesEvent.Uploaded -> context.getString(R.string.files_uploaded, event.name)
            is FilesEvent.Installed -> event.result
            is FilesEvent.Failed -> event.details?.let { "$failedMessage: $it" } ?: failedMessage
        }
        snackbarHostState.showSnackbar(text)
        viewModel.clearEvent()
    }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var createFolderName by remember { mutableStateOf("") }
    var showInstallApkDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showFileOptionsEntry by remember { mutableStateOf<FileEntry?>(null) }
    var renameEntry by remember { mutableStateOf<FileEntry?>(null) }

    // Bytes waiting for the user to pick a destination.
    var pendingExport by remember { mutableStateOf<Pair<String, ByteArray>?>(null) }

    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = displayName(context, uri)
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (bytes == null) {
                snackbarHostState.showSnackbar(failedMessage)
            } else {
                viewModel.push(name, bytes)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val pending = pendingExport
        pendingExport = null
        if (uri == null || pending == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(pending.second) }
                    ?: error("output stream unavailable")
            }.isSuccess
            snackbarHostState.showSnackbar(
                if (ok) context.getString(R.string.files_saved, pending.first) else failedMessage
            )
        }
    }

    fun download(entry: FileEntry) {
        viewModel.pull(entry) { bytes ->
            pendingExport = entry.name to bytes
            exportLauncher.launch(entry.name)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.selectionMode) {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.files_selected_count, uiState.selection.size))
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.files_close))
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::copySelection) {
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.files_copy))
                        }
                        IconButton(onClick = viewModel::cutSelection) {
                            Icon(Icons.Default.ContentCut, contentDescription = stringResource(R.string.files_cut))
                        }
                        IconButton(onClick = viewModel::deleteSelection) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.currentPath,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (uiState.currentPath != "/sdcard") {
                            IconButton(onClick = { viewModel.goUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.files_up))
                            }
                        }
                    },
                    actions = {
                        if (uiState.clipboard.isNotEmpty()) {
                            IconButton(onClick = viewModel::paste) {
                                Icon(
                                    Icons.Default.ContentPaste,
                                    contentDescription = stringResource(R.string.files_paste)
                                )
                            }
                        }
                        IconButton(onClick = { uploadLauncher.launch(arrayOf("*/*")) }) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = stringResource(R.string.files_upload)
                            )
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.files_refresh))
                        }
                        IconButton(onClick = {
                            createFolderName = ""
                            showCreateFolderDialog = true
                        }) {
                            Icon(
                                Icons.Default.CreateNewFolder,
                                contentDescription = stringResource(R.string.files_new_folder)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ExpressiveProgress.contained()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.entries) { entry ->
                        FileItem(
                            entry = entry,
                            selected = entry.path in uiState.selection,
                            selectionMode = uiState.selectionMode,
                            onClick = {
                                if (uiState.selectionMode) {
                                    viewModel.toggleSelection(entry.path)
                                } else if (entry.isDirectory) {
                                    viewModel.navigate(entry.path)
                                } else if (entry.name.endsWith(".apk", ignoreCase = true)) {
                                    showInstallApkDialog = entry.path to entry.name
                                }
                            },
                            onLongClick = {
                                if (!uiState.selectionMode) viewModel.startSelection(entry.path)
                            },
                            onMore = { showFileOptionsEntry = entry }
                        )
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    viewModel.createFolder(uiState.currentPath, name)
                }
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false },
            folderName = createFolderName,
            onFolderNameChange = { createFolderName = it }
        )
    }

    showInstallApkDialog?.let { (path, name) ->
        InstallApkDialog(
            path = path,
            name = name,
            onConfirm = {
                viewModel.installApk(path)
                showInstallApkDialog = null
            },
            onDismiss = { showInstallApkDialog = null }
        )
    }

    showFileOptionsEntry?.let { entry ->
        FileOptionsDialog(
            entry = entry,
            onOpen = {
                if (entry.isDirectory) {
                    viewModel.navigate(entry.path)
                } else if (entry.name.endsWith(".apk", ignoreCase = true)) {
                    viewModel.installApk(entry.path)
                } else {
                    download(entry)
                }
                showFileOptionsEntry = null
            },
            onRename = {
                renameEntry = entry
                showFileOptionsEntry = null
            },
            onCopy = {
                viewModel.startSelection(entry.path)
                viewModel.copySelection()
                showFileOptionsEntry = null
            },
            onCut = {
                viewModel.startSelection(entry.path)
                viewModel.cutSelection()
                showFileOptionsEntry = null
            },
            onDownload = {
                download(entry)
                showFileOptionsEntry = null
            },
            onProperties = {
                viewModel.loadProperties(entry)
                showFileOptionsEntry = null
            },
            onDelete = {
                viewModel.delete(entry.path, entry.name)
                showFileOptionsEntry = null
            },
            onDismiss = { showFileOptionsEntry = null }
        )
    }

    renameEntry?.let { entry ->
        RenameDialog(
            currentName = entry.name,
            onConfirm = { name ->
                viewModel.rename(entry, name)
                renameEntry = null
            },
            onDismiss = { renameEntry = null }
        )
    }

    uiState.propertiesName?.let { name ->
        PropertiesDialog(
            name = name,
            text = uiState.propertiesText,
            loading = uiState.propertiesLoading,
            onDismiss = viewModel::dismissProperties
        )
    }
}

private fun displayName(context: Context, uri: Uri): String {
    val fromProvider = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()
    return fromProvider?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: "upload.bin"
}

@Composable
fun CreateFolderDialog(
    folderName: String,
    onFolderNameChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.files_new_folder)) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = onFolderNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.files_folder_name_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(folderName) }) {
                Text(stringResource(R.string.files_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.files_cancel))
            }
        }
    )
}

@Composable
private fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.files_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.files_new_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && name != currentName,
                onClick = { onConfirm(name) }
            ) { Text(stringResource(R.string.files_rename)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.files_cancel)) }
        }
    )
}

@Composable
private fun PropertiesDialog(
    name: String,
    text: String?,
    loading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            if (loading) {
                Text("…")
            } else if (text == null) {
                Text(stringResource(R.string.files_operation_failed))
            } else {
                // stat -c '%n|%F|%s|%y|%a|%U:%G'
                val parts = text.split('|')
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    propertyRow(R.string.files_property_name, parts.getOrNull(0).orEmpty())
                    propertyRow(R.string.files_property_type, parts.getOrNull(1).orEmpty())
                    propertyRow(
                        R.string.files_property_size,
                        parts.getOrNull(2)?.toLongOrNull()?.let { formatFileSize(it) }.orEmpty()
                    )
                    propertyRow(R.string.files_property_modified, parts.getOrNull(3).orEmpty())
                    propertyRow(R.string.files_property_permissions, parts.getOrNull(4).orEmpty())
                    propertyRow(R.string.files_property_owner, parts.getOrNull(5).orEmpty())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.files_close)) }
        }
    )
}

@Composable
private fun propertyRow(labelRes: Int, value: String) {
    if (value.isBlank()) return
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun InstallApkDialog(
    path: String,
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.files_install_apk)) },
        text = { Text(stringResource(R.string.files_install_apk_confirm, name)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.files_install))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.files_cancel))
            }
        }
    )
}

@Composable
fun FileOptionsDialog(
    entry: FileEntry,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDownload: () -> Unit,
    onProperties: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (entry.isDirectory) {
                    Text(stringResource(R.string.files_is_directory))
                } else {
                    Text(stringResource(R.string.files_size, formatFileSize(entry.size)))
                    Text(stringResource(R.string.files_modified, entry.modified))
                }
                Spacer(Modifier.height(8.dp))
                DialogAction(
                    icon = when {
                        entry.isDirectory -> Icons.Default.Folder
                        entry.name.endsWith(".apk", ignoreCase = true) -> Icons.Default.InstallMobile
                        else -> Icons.Default.Download
                    },
                    label = when {
                        entry.isDirectory -> stringResource(R.string.files_open)
                        entry.name.endsWith(".apk", ignoreCase = true) -> stringResource(R.string.files_install)
                        else -> stringResource(R.string.files_download)
                    },
                    onClick = onOpen
                )
                DialogAction(Icons.Default.Edit, stringResource(R.string.files_rename), onRename)
                DialogAction(Icons.Default.ContentCopy, stringResource(R.string.files_copy), onCopy)
                DialogAction(Icons.Default.ContentCut, stringResource(R.string.files_cut), onCut)
                if (!entry.isDirectory) {
                    DialogAction(
                        Icons.Default.Download,
                        stringResource(R.string.files_download),
                        onDownload
                    )
                }
                DialogAction(Icons.Default.Info, stringResource(R.string.files_properties), onProperties)
                DialogAction(
                    Icons.Default.Delete,
                    stringResource(R.string.files_delete),
                    onDelete,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.files_cancel))
            }
        }
    )
}

@Composable
private fun DialogAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(label, color = tint, modifier = Modifier.weight(1f))
    }
}

@Composable
fun FileItem(
    entry: FileEntry,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = if (entry.isDirectory) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!entry.isDirectory) {
                    Text(
                        text = "${formatFileSize(entry.size)} • ${entry.modified}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (!selectionMode) {
                IconButton(onClick = onMore) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.files_more))
                }
            }
        }
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}
