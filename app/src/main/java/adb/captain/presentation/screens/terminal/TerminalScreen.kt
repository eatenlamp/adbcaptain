package adb.captain.presentation.screens.terminal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import adb.captain.ui.theme.JetBrainsMono
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import androidx.compose.ui.res.stringResource
import adb.captain.R

/**
 * Экран терминала с цветовой подсветкой, историей и командой help.
 */
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val autoCompleteEnabled by viewModel.isAutoCompleteEnabled.collectAsState()
    var command by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.output.size) {
        if (uiState.output.isNotEmpty()) {
            listState.animateScrollToItem(uiState.output.size - 1)
        }
    }

    @Composable
    fun buildHelpText(): String = buildString {
        append(stringResource(R.string.term_help_title))
        append("\n\n")
        append(stringResource(R.string.term_help_intro))
        append(stringResource(R.string.term_help_commands))
        append(stringResource(R.string.term_help_features))
        append(stringResource(R.string.term_help_tips))
    }

    val helpText = buildHelpText()

    fun submit(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        if (trimmed == "help") {
            viewModel.printLocal(cmd, helpText)
        } else {
            viewModel.executeCommand(cmd)
        }
        command = ""
        showHistory = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // Makes the screen resize when keyboard is open
    ) {
        // Beginner Welcome Banner
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.terminal_welcome_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.terminal_welcome_subtitle),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Output Area
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(uiState.output) { annotatedString ->
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMono
                    )
                }
            }
            
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { showHistory = !showHistory }) {
                Icon(Icons.Default.History, contentDescription = stringResource(R.string.term_history_title))
            }
            IconButton(onClick = { viewModel.clearOutput() }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Output")
            }
        }

        // History Panel
        if (showHistory) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.term_history_title),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text(stringResource(R.string.term_history_clear))
                        }
                    }
                    if (uiState.history.isEmpty()) {
                        Text(
                            stringResource(R.string.term_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        val historyItems = remember(uiState.history) {
                            uiState.history
                                .map { it.text }
                                .distinct()
                                .take(30)
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                            items(historyItems) { item ->
                                Surface(
                                    onClick = {
                                        command = item
                                        showHistory = false
                                    },
                                    color = Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = JetBrainsMono,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.term_history_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Quick Commands
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val quickCommands = listOf(
                R.string.cmd_list_apps to "pm list packages -3",
                R.string.cmd_battery_status to "dumpsys battery",
                R.string.cmd_screen_size to "wm size",
                R.string.cmd_device_model to "getprop ro.product.model",
                R.string.cmd_free_space to "df -h",
                R.string.cmd_top_processes to "top -n 1",
                R.string.cmd_event_log to "logcat -d -t 50"
            )
            quickCommands.forEach { (labelRes, cmd) ->
                AssistChip(
                    onClick = { viewModel.executeCommand(cmd) },
                    label = { Text(stringResource(labelRes)) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Auto-complete suggestions from history
        if (autoCompleteEnabled && command.isNotBlank()) {
            val suggestions = remember(command, uiState.history) {
                uiState.history
                    .map { it.text }
                    .distinct()
                    .filter { it.startsWith(command, ignoreCase = true) && it != command }
                    .take(5)
            }
            if (suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { command = suggestion },
                            label = { Text(suggestion, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }

        // Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.terminal_placeholder)) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontFamily = JetBrainsMono)
            )
            
            Spacer(Modifier.width(8.dp))
            
            IconButton(
                onClick = { submit(command) },
                enabled = command.isNotBlank() && !uiState.isLoading
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
