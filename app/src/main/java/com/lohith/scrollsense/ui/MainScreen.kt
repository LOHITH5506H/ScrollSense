package com.lohith.scrollsense.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.imePadding // <-- ADD THIS IMPORT
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lohith.scrollsense.viewmodel.MainViewModel
import com.lohith.scrollsense.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val modalSheetState = rememberModalBottomSheetState()
    val showSettingsSheet = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (showSettingsSheet.value) {
        SettingsSheet(
            onDismiss = {
                coroutineScope.launch {
                    modalSheetState.hide()
                    showSettingsSheet.value = false
                }
            },
            modalSheetState = modalSheetState,
            viewModel = viewModel
        )
    }

    // MODIFIED: Added Modifier.imePadding()
    Scaffold(
        modifier = Modifier.imePadding(), // <-- ADD THIS
        topBar = {
            TopAppBar(
                title = { Text("ScrollSense") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            modalSheetState.show()
                            showSettingsSheet.value = true
                        }
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {}
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController, startDestination = "dashboard") {
                composable("dashboard") { DashboardScreen(viewModel) }
                composable("logs") { LogsScreen(viewModel) }
                composable("analytics") { AnalyticsScreen(viewModel) }
                composable("settings") { SettingsScreen(viewModel) } // This is the screen you're editing
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        BottomNavItem.items.forEach { item ->
            AddItem(
                item = item,
                currentDestination = currentDestination,
                navController = navController
            )
        }
    }
}

@Composable
fun RowScope.AddItem(
    item: BottomNavItem,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    NavigationBarItem(
        label = { Text(item.title) },
        icon = { Icon(item.icon, contentDescription = "Navigation Icon") },
        selected = currentDestination?.hierarchy?.any {
            it.route == item.route
        } == true,
        onClick = {
            navController.navigate(item.route) {
                // Pop up to the start destination of the graph to
                // avoid building up a large stack of destinations
                // on the back stack as users select items
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                // Avoid multiple copies of the same destination when
                // re-selecting the same item
                launchSingleTop = true
                // Restore state when re-selecting a previously selected item
                restoreState = true
            }
        }
    )
}

sealed class BottomNavItem(val title: String, val icon: ImageVector, val route: String) {
    object Dashboard : BottomNavItem("Dashboard", Icons.Default.Dashboard, "dashboard")
    object Logs : BottomNavItem("Logs", Icons.Default.List, "logs")
    object Analytics : BottomNavItem("Analytics", Icons.Default.Analytics, "analytics")
    object Settings : BottomNavItem("Settings", Icons.Default.Settings, "settings") // This is the screen you're editing

    companion object {
        val items = listOf(Dashboard, Logs, Analytics, Settings)
    }
}