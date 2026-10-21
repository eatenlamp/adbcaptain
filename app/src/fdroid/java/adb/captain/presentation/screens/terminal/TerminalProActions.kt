/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.terminal

import androidx.compose.runtime.Composable

/**
 * The FOSS build has no paid actions in the terminal.
 */
@Composable
fun TerminalProActions(onExportMarkdown: () -> String) {
    // Nothing to show in the FOSS build.
}
