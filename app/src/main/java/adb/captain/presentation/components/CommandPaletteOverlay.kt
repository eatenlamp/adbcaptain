/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import adb.captain.R
import adb.captain.domain.model.Macro
import adb.captain.ui.theme.JetBrainsMono

/** One searchable action in the palette. */
data class PaletteItem(
    val title: String,
    val subtitle: String,
    val onRun: () -> Unit
)

/**
 * Full-screen quick launcher: screens, saved macros and a list of common shell
 * commands, all reachable by typing a few letters.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommandPaletteOverlay(
    screens: List<PaletteItem>,
    macros: List<Macro>,
    onOpenMacros: () -> Unit,
    onRunCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    // Predefined shell shortcuts; only shown while the user has not typed a
    // command-shaped string yet.
    val shortcuts = remember {
        listOf(
            "clear logcat" to "logcat -c",
            "logcat errors" to "logcat -d *:E",
            "app pid" to "pidof",
            "screen size" to "wm size",
            "top processes" to "top -n 1",
            "free space" to "df -h",
            "battery" to "dumpsys battery",
            "thermal" to "dumpsys thermalservice",
            "event log" to "logcat -b events -d -t 50",
            "who am i" to "id"
        )
    }

    val trimmed = query.trim()
    val macroItems = macros
        .filter { trimmed.isBlank() || it.name.contains(trimmed, ignoreCase = true) }
        .map { macro ->
            PaletteItem(
                title = macro.name,
                subtitle = macro.commands.joinToString(" ; "),
                onRun = onOpenMacros
            )
        }
    val screenItems = screens.filter { it.title.contains(trimmed, ignoreCase = true) }
    val shortcutItems = shortcuts
        .filter { trimmed.isBlank() || it.first.contains(trimmed, ignoreCase = true) || it.second.contains(trimmed, ignoreCase = true) }
        .map { (label, cmd) -> PaletteItem(label, cmd) { onRunCommand(cmd) } }

    val rows: List<PaletteItem> = buildList {
        addAll(screenItems)
        addAll(macroItems)
        addAll(shortcutItems)
    }

    Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.palette_hint)) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono)
                    )
                    if (rows.isEmpty()) {
                        Text(
                            text = stringResource(R.string.palette_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                            items(rows) { item ->
                                PaletteRow(item = item, onClick = {
                                    item.onRun()
                                    onDismiss()
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
    }
}

@Composable
private fun PaletteRow(item: PaletteItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = JetBrainsMono,
                maxLines = 2
            )
        }
        Icon(Icons.Default.PlayArrow, contentDescription = null)
    }
}