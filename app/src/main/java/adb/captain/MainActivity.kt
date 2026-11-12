/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import adb.captain.data.local.MacroShortcuts
import adb.captain.presentation.components.CommandPaletteOverlay
import adb.captain.presentation.components.PaletteItem
import adb.captain.presentation.components.PaletteViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import adb.captain.presentation.navigation.Screen
import adb.captain.presentation.screens.setup.SetupScreen
import adb.captain.presentation.screens.terminal.TerminalScreen
import adb.captain.presentation.screens.settings.SettingsViewModel
import adb.captain.presentation.screens.devices.DevicesScreen
import adb.captain.presentation.screens.apps.AppsScreen
import adb.captain.presentation.screens.logcat.LogcatScreen
import adb.captain.presentation.screens.sideload.SideloadScreen
import adb.captain.presentation.screens.help.HelpScreen
import adb.captain.presentation.screens.settings.SettingsScreen
import adb.captain.presentation.screens.files.FilesScreen
import adb.captain.presentation.screens.monitor.MonitorScreen
import adb.captain.presentation.screens.debug.DebugScreen
import adb.captain.presentation.screens.wireless.WirelessAdbScreen
import adb.captain.presentation.screens.more.MoreScreen
import adb.captain.ui.theme.ADBCaptainTheme
import adb.captain.ui.theme.AppTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import rikka.shizuku.Shizuku
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), Shizuku.OnRequestPermissionResultListener {

    @Inject
    lateinit var macroShortcuts: MacroShortcuts

    /** Set when the app is opened from a macro shortcut. */
    private val pendingMacroId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShortcutIntent(intent)

        if (ShizukuManager.isShizukuRunning() && !ShizukuManager.checkShizukuPermission()) {
            ShizukuManager.requestShizukuPermission(this)
        }
        Shizuku.addRequestPermissionResultListener(this)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val language by settingsViewModel.language.collectAsState()
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
            val appThemeName by settingsViewModel.appTheme.collectAsState()
            val appTheme = remember(appThemeName) {
                runCatching { AppTheme.valueOf(appThemeName) }.getOrDefault(AppTheme.TealNavy)
            }

            // Apply language change
            LaunchedEffect(language) {
                val appLocales = LocaleListCompat.forLanguageTags(language)
                AppCompatDelegate.setApplicationLocales(appLocales)
            }

            ADBCaptainTheme(darkTheme = isDarkTheme, appTheme = appTheme, dynamicColor = true) {
                // Follows Shizuku binder events (no polling), so the UI unlocks instantly.
                val isShizukuActive by ShizukuManager.isRunning.collectAsState()

                LaunchedEffect(Unit) {
                    ShizukuManager.refresh()
                }

                if (!isShizukuActive) {
                    SetupScreen()
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val currentScreen = Screen.byRoute(currentDestination?.route)

                    // Show the tutorial on first launch (reads the persisted value once)
                    LaunchedEffect(Unit) {
                        if (!settingsViewModel.hasSeenTutorialNow()) {
                            navController.navigate("help")
                            settingsViewModel.markTutorialSeen()
                        }
                    }

                    fun navigateTo(route: String) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    fun isSelected(screen: Screen): Boolean {
                        if (screen == Screen.More) {
                            val route = currentDestination?.route
                            return route != null && Screen.secondaryScreens.any { it.route == route }
                        }
                        return currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    }

                    var showPalette by remember { mutableStateOf(false) }
                    var pendingTerminalCommand by remember { mutableStateOf<String?>(null) }
                    val paletteViewModel: PaletteViewModel = hiltViewModel()
                    val paletteMacros by paletteViewModel.macros.collectAsStateWithLifecycle()
                    val shortcutMacroId by pendingMacroId.collectAsState()

                    // Keep the launcher shortcuts in sync with the saved macros.
                    LaunchedEffect(paletteMacros) {
                        macroShortcuts.sync(paletteMacros)
                    }

                    // A shortcut or an external intent asks for one macro to run.
                    LaunchedEffect(shortcutMacroId) {
                        if (shortcutMacroId != null) {
                            navigateTo(Screen.Debug.route)
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        // Phones get a bottom bar, tablets and foldables a rail.
                        val expandedLayout = maxWidth >= 600.dp

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                if (currentScreen != null) {
                                    CenterAlignedTopAppBar(
                                        title = { Text(stringResource(currentScreen.titleRes)) },
                                        navigationIcon = {
                                            IconButton(onClick = { showPalette = true }) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = stringResource(R.string.palette_open)
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            },
                            bottomBar = {
                                if (!expandedLayout) {
                                    NavigationBar {
                                        Screen.primaryScreens.forEach { screen ->
                                            NavigationBarItem(
                                                icon = { Icon(screen.icon, contentDescription = null) },
                                                label = {
                                                    // "Приложения" does not fit at the default
                                                    // label size and used to break mid-word.
                                                    Text(
                                                        text = stringResource(screen.titleRes),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                selected = isSelected(screen),
                                                onClick = { navigateTo(screen.route) }
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (expandedLayout) {
                                    NavigationRail {
                                        Spacer(Modifier.height(8.dp))
                                        // Every destination fits on tall tablets; the scroll
                                        // keeps short landscape windows usable.
                                        Column(
                                            modifier = Modifier.verticalScroll(rememberScrollState())
                                        ) {
                                            Screen.all.forEach { screen ->
                                                NavigationRailItem(
                                                    icon = { Icon(screen.icon, contentDescription = null) },
                                                    label = { Text(stringResource(screen.titleRes)) },
                                                    selected = isSelected(screen),
                                                    onClick = { navigateTo(screen.route) }
                                                )
                                            }
                                        }
                                    }
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = Screen.Terminal.route,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    composable(Screen.Terminal.route) {
                                        TerminalScreen(initialCommand = pendingTerminalCommand)
                                    }
                                    composable(Screen.Devices.route) {
                                        DevicesScreen(onOpenWireless = { navController.navigate("wireless") })
                                    }
                                    composable(Screen.Apps.route) { AppsScreen() }
                                    composable(Screen.Logcat.route) { LogcatScreen() }
                                    composable(Screen.Sideload.route) { SideloadScreen() }
                                    composable(Screen.Files.route) { FilesScreen() }
                                    composable(Screen.Monitor.route) { MonitorScreen() }
                                    composable(Screen.Debug.route) {
                                        DebugScreen(
                                            initialMacroId = shortcutMacroId,
                                            onInitialMacroHandled = { pendingMacroId.value = null }
                                        )
                                    }
                                    composable(Screen.Settings.route) {
                                        SettingsScreen(onOpenHelp = { navController.navigate("help") })
                                    }
                                    composable(Screen.More.route) {
                                        MoreScreen(
                                            onOpenScreen = { navigateTo(it.route) },
                                            onOpenWireless = { navController.navigate("wireless") },
                                            onOpenHelp = { navController.navigate("help") }
                                        )
                                    }
                                    composable("help") { HelpScreen(onBack = { navController.popBackStack() }) }
                                    composable("wireless") { WirelessAdbScreen(onBack = { navController.popBackStack() }) }
                                }
                            }
                        }
                    }

                    if (showPalette) {
                        CommandPaletteOverlay(
                            screens = Screen.all.map { screen ->
                                PaletteItem(
                                    title = stringResource(screen.titleRes),
                                    subtitle = screen.route,
                                    onRun = { navigateTo(screen.route) }
                                )
                            },
                            macros = paletteMacros,
                            onRunMacro = { macro -> pendingMacroId.value = macro.id },
                            onRunCommand = { cmd ->
                                navigateTo(Screen.Terminal.route)
                                pendingTerminalCommand = cmd
                            },
                            onDismiss = { showPalette = false }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        if (intent?.action != MacroShortcuts.ACTION_RUN_MACRO) return
        val id = intent.getLongExtra(MacroShortcuts.EXTRA_MACRO_ID, 0L)
        if (id > 0L) pendingMacroId.value = id
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        // Handle if needed
    }
}
