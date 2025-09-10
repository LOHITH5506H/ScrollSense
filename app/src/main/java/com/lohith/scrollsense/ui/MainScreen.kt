package com.lohith.scrollsense.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lohith.scrollsense.viewmodel.MainViewModel

// Data class is better for rememberSaveable than a sealed class object
data class Screen(val route: String, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    Screen("dashboard", "Dashboard", Icons.Default.Dashboard),
    Screen("analytics", "Analytics", Icons.Default.Analytics),
    Screen("logs", "Logs", Icons.AutoMirrored.Filled.List),
    Screen("settings", "Settings", Icons.Default.Settings)
)

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = viewModel()
    // FIX: Save the route string of the current screen. This is saveable.
    var currentScreenRoute by rememberSaveable { mutableStateOf(Screen("dashboard", "", Icons.Default.Dashboard).route) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentScreenRoute == screen.route,
                        onClick = { currentScreenRoute = screen.route }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreenRoute) {
            "dashboard" -> DashboardScreen(viewModel = viewModel, paddingValues = innerPadding)
            "analytics" -> AnalyticsScreen(viewModel = viewModel, paddingValues = innerPadding)
            "logs" -> LogsScreen(viewModel = viewModel, paddingValues = innerPadding)
            "settings" -> SettingsScreen(paddingValues = innerPadding)
        }
    }
}