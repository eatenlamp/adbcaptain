/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import adb.captain.R

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Terminal : Screen("terminal", R.string.tab_terminal, Icons.Default.Terminal)
    object Devices : Screen("devices", R.string.tab_devices, Icons.Default.Devices)
    object Apps : Screen("apps", R.string.tab_apps, Icons.Default.Apps)
    object Logcat : Screen("logcat", R.string.tab_logcat, Icons.AutoMirrored.Filled.ListAlt)
    object Sideload : Screen("sideload", R.string.tab_sideload, Icons.Default.SystemUpdate)
    object Files : Screen("files", R.string.tab_files, Icons.Default.Folder)
    object Monitor : Screen("monitor", R.string.tab_monitor, Icons.Default.ShowChart)
    object Settings : Screen("settings", R.string.tab_settings, Icons.Default.Settings)
}
