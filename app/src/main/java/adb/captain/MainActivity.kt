package adb.captain

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
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
import adb.captain.ui.theme.ADBCaptainTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), Shizuku.OnRequestPermissionResultListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ShizukuManager.isShizukuRunning() && !ShizukuManager.checkShizukuPermission()) {
            ShizukuManager.requestShizukuPermission(this)
        }
        Shizuku.addRequestPermissionResultListener(this)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val language by settingsViewModel.language.collectAsState()
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

            // Apply language change
            LaunchedEffect(language) {
                val appLocales = LocaleListCompat.forLanguageTags(language)
                AppCompatDelegate.setApplicationLocales(appLocales)
            }

            ADBCaptainTheme(darkTheme = isDarkTheme) {
                var isShizukuActive by remember { mutableStateOf(ShizukuManager.isShizukuRunning()) }

                // Status polling for automatic UI unlock
                LaunchedEffect(Unit) {
                    while(true) {
                        isShizukuActive = ShizukuManager.isShizukuRunning()
                        kotlinx.coroutines.delay(2000)
                    }
                }

                if (!isShizukuActive) {
                    SetupScreen()
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    // Show the tutorial on first launch (reads the persisted value once)
                    LaunchedEffect(Unit) {
                        if (!settingsViewModel.hasSeenTutorialNow()) {
                            navController.navigate("help")
                            settingsViewModel.markTutorialSeen()
                        }
                    }
                    
                    val screens = listOf(
                        Screen.Terminal,
                        Screen.Devices,
                        Screen.Apps,
                        Screen.Logcat,
                        Screen.Sideload,
                        Screen.Files,
                        Screen.Monitor,
                        Screen.Settings
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            val currentScreen = screens.find { it.route == currentDestination?.route }
                            if (currentScreen != null) {
                                CenterAlignedTopAppBar(
                                    title = { Text(stringResource(currentScreen.titleRes)) },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            NavigationBar {
                                screens.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = null) },
                                        label = { Text(stringResource(screen.titleRes)) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Terminal.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Terminal.route) { TerminalScreen() }
                            composable(Screen.Devices.route) { DevicesScreen() }
                            composable(Screen.Apps.route) { AppsScreen() }
                            composable(Screen.Logcat.route) { LogcatScreen() }
                            composable(Screen.Sideload.route) { SideloadScreen() }
                            composable(Screen.Files.route) { FilesScreen() }
                            composable(Screen.Monitor.route) { MonitorScreen() }
                            composable(Screen.Settings.route) { SettingsScreen(onOpenHelp = { navController.navigate("help") }) }
                            composable("help") { HelpScreen(onBack = { navController.popBackStack() }) }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        // Handle if needed
    }
}
