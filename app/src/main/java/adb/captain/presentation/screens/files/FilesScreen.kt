package adb.captain.presentation.screens.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import adb.captain.domain.model.FileEntry

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: FilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var createFolderName by remember { mutableStateOf("") }
    var showInstallApkDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showFileOptionsEntry by remember { mutableStateOf<FileEntry?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.files_up))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.files_refresh))
                    }
                    IconButton(onClick = {
                        createFolderName = ""
                        showCreateFolderDialog = true
                    }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(R.string.files_new_folder))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
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
                            onClick = {
                                if (entry.isDirectory) {
                                    viewModel.navigate(entry.path)
                                } else if (entry.name.endsWith(".apk", ignoreCase = true)) {
                                    showInstallApkDialog = Pair(entry.path, entry.name)
                                } else {
                                    viewModel.navigate(entry.path)
                                }
                            },
                            onDelete = { viewModel.delete(entry.path, entry.name) },
                            onLongClick = { showFileOptionsEntry = entry }
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
                } else {
                    viewModel.installApk(entry.path)
                }
                showFileOptionsEntry = null
            },
            onDelete = {
                viewModel.delete(entry.path, entry.name)
                showFileOptionsEntry = null
            },
            onDismiss = { showFileOptionsEntry = null }
        )
    }
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
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name) },
        text = {
            Column {
                if (entry.isDirectory) {
                    Text(stringResource(R.string.files_is_directory))
                } else {
                    Text(stringResource(R.string.files_size, formatFileSize(entry.size)))
                    Text(stringResource(R.string.files_modified, entry.modified))
                }
            }
        },
        confirmButton = {
            if (entry.isDirectory || entry.name.endsWith(".apk", ignoreCase = true)) {
                Button(onClick = onOpen) {
                    Text(if (entry.isDirectory) stringResource(R.string.files_open) else stringResource(R.string.files_install))
                }
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
fun FileItem(
    entry: FileEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
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
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.files_more))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (entry.isDirectory) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.files_open)) },
                            onClick = { menuExpanded = false; onClick() }
                        )
                    } else if (entry.name.endsWith(".apk", ignoreCase = true)) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.files_install)) },
                            onClick = { menuExpanded = false; onClick() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.files_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
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