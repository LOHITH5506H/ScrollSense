package com.lohith.scrollsense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lohith.scrollsense.viewmodel.MainViewModel

// Defines the screens in the app
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Analytics : Screen("analytics", "Analytics", Icons.Filled.Info)
    object Logs : Screen("logs", "Logs", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun MainScreen() {
    // Create one instance of the ViewModel to be shared by all screens
    val viewModel: MainViewModel = viewModel()
    var currentScreenRoute by rememberSaveable { mutableStateOf(Screen.Dashboard.route) }
    val screens = listOf(Screen.Dashboard, Screen.Analytics, Screen.Logs, Screen.Settings)

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFF0F0F0)) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentScreenRoute == screen.route,
                        onClick = { currentScreenRoute = screen.route },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF512DA8),
                            selectedTextColor = Color(0xFF512DA8),
                            indicatorColor = Color(0xFFEDE7F6),
                            unselectedIconColor = Color(0xFF616161),
                            unselectedTextColor = Color(0xFF616161)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .background(Color(0xFFF5F5F5))) {
            when (currentScreenRoute) {
                Screen.Dashboard.route -> DashboardScreen(viewModel)
                Screen.Analytics.route -> AnalyticsScreen(viewModel)
                // We will create Logs and Settings screens in the next step
                Screen.Logs.route -> LogsScreen(viewModel)
                Screen.Settings.route -> SettingsScreen()
            }
        }
    }
}