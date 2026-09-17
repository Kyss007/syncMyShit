package com.syncmyshit.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.syncmyshit.app.service.EmulatorWatcherService
import com.syncmyshit.app.service.SyncWorkManagerHelper
import com.syncmyshit.app.ui.navigation.Screen
import com.syncmyshit.app.ui.screens.DashboardScreen
import com.syncmyshit.app.ui.screens.EmulatorsScreen
import com.syncmyshit.app.ui.screens.HistoryScreen
import com.syncmyshit.app.ui.screens.SettingsScreen
import com.syncmyshit.app.ui.screens.SetupWizardScreen
import com.syncmyshit.app.ui.theme.DarkBorder
import com.syncmyshit.app.ui.theme.DarkSurface
import com.syncmyshit.app.ui.theme.NeonCyan
import com.syncmyshit.app.ui.theme.SyncMyShitTheme
import com.syncmyshit.app.ui.theme.TextMuted
import com.syncmyshit.app.ui.theme.TextSecondary
import com.syncmyshit.app.ui.viewmodel.MainViewModel
import com.syncmyshit.app.ui.viewmodel.SetupViewModel
import com.syncmyshit.app.utils.StorageAccessHelper

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val setupViewModel: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleOAuthIntent(intent)

        setContent {
            SyncMyShitTheme {
                val navController = rememberNavController()
                val isSetupCompleted by mainViewModel.isSetupCompleted.collectAsState()
                val autoSync by mainViewModel.autoSyncEnabled.collectAsState()
                val detectApp by mainViewModel.detectAppSwitch.collectAsState()
                val syncInterval by mainViewModel.syncIntervalMinutes.collectAsState()
                val wifiOnly by mainViewModel.wifiOnly.collectAsState()

                LaunchedEffect(isSetupCompleted, autoSync, detectApp) {
                    if (isSetupCompleted && autoSync) {
                        SyncWorkManagerHelper.schedulePeriodicSync(
                            this@MainActivity,
                            syncInterval.toLong(),
                            wifiOnly
                        )

                        if (detectApp && StorageAccessHelper.hasUsageStatsPermission(this@MainActivity)) {
                            val serviceIntent = Intent(this@MainActivity, EmulatorWatcherService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(serviceIntent)
                            } else {
                                startService(serviceIntent)
                            }
                        }
                    }
                }

                val startDestination = if (isSetupCompleted) Screen.Dashboard.route else Screen.SetupWizard.route

                AppNavigation(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    setupViewModel = setupViewModel,
                    startDestination = startDestination
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.syncmyshit.app") {
            val app = application as SyncApplication
            lifecycleScope.launch {
                val result = app.authManager.handleOAuthCallback(uri)
                if (result.isSuccess) {
                    val email = result.getOrNull()
                    setupViewModel.onWebAuthSuccess(email)
                } else {
                    setupViewModel.setAuthError("Web Login Error: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    setupViewModel: SetupViewModel,
    startDestination: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isWizard = currentRoute == Screen.SetupWizard.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isWizard) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        Triple(Screen.Dashboard, Icons.Default.Dashboard, "Dashboard"),
                        Triple(Screen.Emulators, Icons.Default.SportsEsports, "Emulators"),
                        Triple(Screen.History, Icons.Default.History, "History"),
                        Triple(Screen.Settings, Icons.Default.Settings, "Settings")
                    )

                    items.forEach { (screen, icon, label) ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = DarkBorder
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.SetupWizard.route) {
                SetupWizardScreen(
                    viewModel = setupViewModel,
                    onSetupComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.SetupWizard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = mainViewModel,
                    onNavigateToEmulators = { navController.navigate(Screen.Emulators.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                )
            }

            composable(Screen.Emulators.route) {
                EmulatorsScreen(viewModel = mainViewModel)
            }

            composable(Screen.History.route) {
                HistoryScreen(viewModel = mainViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = mainViewModel,
                    onSignOut = {
                        navController.navigate(Screen.SetupWizard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
