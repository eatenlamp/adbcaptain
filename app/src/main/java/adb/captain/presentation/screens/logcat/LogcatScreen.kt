package adb.captain.presentation.screens.logcat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import adb.captain.ui.theme.JetBrainsMono
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.R
import adb.captain.domain.model.LogEntry
import adb.captain.domain.model.LogLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogcatScreen(
    viewModel: LogcatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboard.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.logcat_copied)

    // In-memory filtering by level and text/tag query
    val visibleLogs = remember(uiState.logs, uiState.query, uiState.selectedLevel) {
        uiState.logs.filter { entry ->
            (uiState.selectedLevel == null || entry.level == uiState.selectedLevel) &&
                (uiState.query.isBlank() ||
                    entry.message.contains(uiState.query, ignoreCase = true) ||
                    entry.tag.contains(uiState.query, ignoreCase = true))
        }
    }

    // Smart auto-scroll: only follow the stream while the user is at the bottom
    val isAtBottom by remember {
        derivedStateOf { !listState.canScrollForward }
    }

    LaunchedEffect(visibleLogs.size, isAtBottom) {
        if (isAtBottom && visibleLogs.isNotEmpty()) {
            listState.animateScrollToItem(visibleLogs.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (!isAtBottom) {
                    SmallFloatingActionButton(
                        onClick = {
                            if (visibleLogs.isNotEmpty()) {
                                scope.launch { listState.animateScrollToItem(visibleLogs.size - 1) }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.logcat_jump_to_bottom)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                SmallFloatingActionButton(
                    onClick = { viewModel.clearLogs() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.logcat_clear))
                }
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = { viewModel.toggleStream() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = stringResource(R.string.logcat_toggle)
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search field
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.logcat_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontFamily = JetBrainsMono)
            )

            // Level filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                LevelChip(
                    label = stringResource(R.string.logcat_level_all),
                    selected = uiState.selectedLevel == null,
                    color = MaterialTheme.colorScheme.onSurface
                ) { viewModel.setLevel(null) }
                LogLevel.entries.forEach { level ->
                    LevelChip(
                        label = level.name.first().toString(),
                        selected = uiState.selectedLevel == level,
                        color = logLevelColor(level)
                    ) { viewModel.setLevel(level) }
                }
            }

            HorizontalDivider(Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

            if (visibleLogs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.logcat_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(visibleLogs, key = { it.id }) { entry ->
                        LogItem(
                            entry = entry,
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(android.content.ClipData.newPlainText("logcat", "${entry.timestamp} ${entry.level.name.first()}/${entry.tag}: ${entry.message}"))
                                    )
                                    snackbarHostState.showSnackbar(copiedMessage)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else color,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier.padding(end = 4.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogItem(entry: LogEntry, onClick: () -> Unit) {
    val color = logLevelColor(entry.level)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = color.copy(alpha = 0.2f),
                contentColor = color,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = entry.level.name.first().toString(),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = entry.timestamp,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMono
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (entry.pid > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "PID:${entry.pid}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = JetBrainsMono
                )
            }
        }
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = JetBrainsMono
        )
    }
}

fun logLevelColor(level: LogLevel): Color = when (level) {
    LogLevel.VERBOSE -> Color(0xFF9E9E9E)
    LogLevel.DEBUG -> Color(0xFF42A5F5)
    LogLevel.INFO -> Color(0xFF4CAF50)
    LogLevel.WARN -> Color(0xFFFFB300)
    LogLevel.ERROR, LogLevel.FATAL -> Color(0xFFF44336)
}
