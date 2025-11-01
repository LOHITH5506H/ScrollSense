package com.lohith.scrollsense.ui

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lohith.scrollsense.R
import com.lohith.scrollsense.viewmodel.MainViewModel
import com.lohith.scrollsense.viewmodel.InsightsViewModel

// Defines the screens in the app
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Analytics : Screen("analytics", "Analytics", Icons.Filled.Info)
    object Logs : Screen("logs", "Logs", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "Insights", Icons.Filled.Info) // Renamed for clarity
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val mainViewModel: MainViewModel = viewModel()
    // Create an instance of the InsightsViewModel here
    val insightsViewModel: InsightsViewModel = viewModel()

    var currentScreenRoute by rememberSaveable { mutableStateOf(Screen.Dashboard.route) }
    val screens = listOf(Screen.Dashboard, Screen.Analytics, Screen.Logs, Screen.Settings)
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            // Use CenterAlignedTopAppBar which is available in current Material3
            CenterAlignedTopAppBar(
                title = {
                    val title = when (currentScreenRoute) {
                        Screen.Dashboard.route -> stringResource(id = R.string.nav_dashboard)
                        Screen.Analytics.route -> stringResource(id = R.string.nav_detailed_stats)
                        Screen.Logs.route -> "Logs"
                        else -> stringResource(id = R.string.settings_title)
                    }
                    Text(title)
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(id = R.string.settings_title))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFF0F0F0)) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = {
                            if (screen.route == "settings") {
                                Text(stringResource(id = R.string.nav_settings))
                            } else {
                                Text(screen.label)
                            }
                        },
                        selected = currentScreenRoute == screen.route,
                        onClick = {
                            if (screen.route == Screen.Analytics.route) {
                                val intent = Intent(context, EnhancedAnalyticsActivity::class.java)
                                context.startActivity(intent)
                            } else {
                                currentScreenRoute = screen.route
                            }
                        },
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
                Screen.Dashboard.route -> DashboardScreen(mainViewModel)
                Screen.Analytics.route -> {
                    DashboardScreen(mainViewModel)
                }
                Screen.Logs.route -> LogsScreen(mainViewModel)
                // Pass the insightsViewModel to the InsightsScreen
                Screen.Settings.route -> InsightsScreen()
            }
        }

        if (showSettings) {
            SettingsSheet(viewModel = mainViewModel, onDismiss = { showSettings = false })
        }
    }
}