/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.terminal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import adb.captain.R
import adb.captain.presentation.components.shareFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Store only action: export the terminal session as a Markdown file.
 */
@Composable
fun TerminalProActions(onExportMarkdown: () -> String) {
    val context = LocalContext.current

    ToolBarAction(
        onClick = {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            shareFile(
                context = context,
                content = onExportMarkdown(),
                fileName = "adb-captain-terminal-$stamp.md",
                mimeType = "text/markdown"
            )
        },
        icon = Icons.AutoMirrored.Filled.NoteAdd,
        contentDescription = context.getString(R.string.term_export_markdown)
    )
}
