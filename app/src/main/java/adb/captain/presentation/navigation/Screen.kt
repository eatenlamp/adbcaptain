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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import adb.captain.R

/**
 * Every destination of the app.
 *
 * [primary] screens get a slot in the compact bottom bar; the rest live behind
 * the More tab on phones and in the navigation rail on tablets.
 */
sealed class Screen(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
    val primary: Boolean = false
) {
    object Terminal : Screen("terminal", R.string.tab_terminal, Icons.Default.Terminal, primary = true)
    object Devices : Screen("devices", R.string.tab_devices, Icons.Default.Devices)
    object Apps : Screen("apps", R.string.tab_apps, Icons.Default.Apps, primary = true)
    object Logcat : Screen("logcat", R.string.tab_logcat, Icons.AutoMirrored.Filled.ListAlt, primary = true)
    object Sideload : Screen("sideload", R.string.tab_sideload, Icons.Default.SystemUpdate)
    object Files : Screen("files", R.string.tab_files, Icons.Default.Folder)
    object Monitor : Screen("monitor", R.string.tab_monitor, Icons.AutoMirrored.Filled.ShowChart)
    object Debug : Screen("debug", R.string.tab_debug, Icons.Default.BugReport, primary = true)
    object Settings : Screen("settings", R.string.tab_settings, Icons.Default.Settings)
    object More : Screen("more", R.string.tab_more, Icons.Default.MoreHoriz, primary = true)

    companion object {
        /** Screens that own a real feature screen, in navigation order. */
        val all: List<Screen> = listOf(
            Terminal,
            Devices,
            Apps,
            Logcat,
            Sideload,
            Files,
            Monitor,
            Debug,
            Settings
        )

        /** Bottom bar / rail order, with More appended last. */
        val primaryScreens: List<Screen> = all.filter { it.primary } + More

        /** Screens hidden behind More on compact screens. */
        val secondaryScreens: List<Screen> = all.filterNot { it.primary }

        fun byRoute(route: String?): Screen? = all.firstOrNull { it.route == route }
            ?: More.takeIf { it.route == route }
    }
}
