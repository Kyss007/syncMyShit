package com.syncmyshit.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Emulators : Screen("emulators", "Emulators")
    data object History : Screen("history", "History")
    data object Settings : Screen("settings", "Settings")
    data object SetupWizard : Screen("setup_wizard", "Setup Wizard")
}
