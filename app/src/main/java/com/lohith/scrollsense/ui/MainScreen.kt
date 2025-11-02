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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lohith.scrollsense.util.PreferencesManager
import com.lohith.scrollsense.viewmodel.InsightsViewModel
import com.lohith.scrollsense.viewmodel.MainViewModel

// Import the screens
import com.lohith.scrollsense.ui.DashboardScreen
import com.lohith.scrollsense.ui.EnhancedAnalyticsActivity
import com.lohith.scrollsense.ui.InsightsScreen
import com.lohith.scrollsense.ui.LogsScreen
// We no longer import SettingsScreen, we import SettingsSheet
import com.lohith.scrollsense.ui.SettingsSheet

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Analytics : Screen("analytics", "Analytics", Icons.Filled.Info)
    object Logs : Screen("logs", "Logs", Icons.AutoMirrored.Filled.List)
    object Insights : Screen("insights", "Insights", Icons.Filled.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    preferencesManager: PreferencesManager,
    onNavigateToAccessibilitySettings: () -> Unit,
    onNavigateToUsageStatsSettings: () -> Unit
) {
    val mainViewModel: MainViewModel = viewModel()
    val insightsViewModel: InsightsViewModel = viewModel()
    val context = LocalContext.current

    var currentScreenRoute by rememberSaveable { mutableStateOf(Screen.Dashboard.route) }
    val screens = listOf(Screen.Dashboard, Screen.Analytics, Screen.Logs, Screen.Insights)

    // ------------------------------------------------------------------
    // FIX: Re-add the code for the settings bottom sheet
    // ------------------------------------------------------------------
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    // ------------------------------------------------------------------


    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = {
                    // Title logic is simplified, settings is no longer a "screen"
                    val title = screens.find { it.route == currentScreenRoute }?.label ?: "ScrollSense"
                    Text(title)
                },
                actions = {
                    // ------------------------------------------------------------------
                    // FIX: Make the icon show the bottom sheet
                    // ------------------------------------------------------------------
                    IconButton(onClick = {
                        showBottomSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF0F0F0)
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFF0F0F0)) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
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
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            when (currentScreenRoute) {
                Screen.Dashboard.route -> DashboardScreen(mainViewModel)
                Screen.Analytics.route -> DashboardScreen(mainViewModel)
                Screen.Logs.route -> LogsScreen(mainViewModel)
                Screen.Insights.route -> InsightsScreen()
                // We removed the "settings_route" as it's no longer a screen
            }
        }

        // ------------------------------------------------------------------
        // FIX: Add the ModalBottomSheet composable here
        // ------------------------------------------------------------------
        if (showBottomSheet) {
            SettingsSheet(
                viewModel = mainViewModel,
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}