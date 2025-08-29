package com.lohith.scrollsense

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lohith.scrollsense.ui.UsageLogScreen
import com.lohith.scrollsense.ui.theme.ScrollSenseTheme
import com.lohith.scrollsense.util.AccessibilityServiceHelper
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScrollSenseTheme {
                val ctx = this@MainActivity
                var usagePermission by remember { mutableStateOf(false) }
                var accessibilityEnabled by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    // initial checks
                    usagePermission = UsageStatsHelper.isUsagePermissionGranted(ctx)
                    accessibilityEnabled = AccessibilityServiceHelper.isServiceEnabled(ctx)
                    if (!usagePermission) {
                        Toast.makeText(ctx, "Grant Usage Access permission", Toast.LENGTH_LONG).show()
                        UsageStatsHelper.openUsageSettings(ctx)
                    }
                    if (!accessibilityEnabled) {
                        // gentle toast; dedicated UI shown below
                        Toast.makeText(ctx, "Enable ScrollSense Accessibility Service", Toast.LENGTH_LONG).show()
                    }
                }

                androidx.compose.material3.Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        when {
                            !usagePermission -> PermissionInstructions(
                                title = "Usage Access Required",
                                message = "The app needs Usage Access to aggregate durations.",
                                actionLabel = "Open Settings",
                                onAction = { UsageStatsHelper.openUsageSettings(ctx) }
                            )
                            !accessibilityEnabled -> PermissionInstructions(
                                title = "Enable Accessibility Service",
                                message = "Turn on ScrollSense service to log app sessions and screen titles.",
                                actionLabel = "Open Accessibility",
                                onAction = { AccessibilityServiceHelper.openAccessibilitySettings(ctx) }
                            )
                            else -> UsageLogScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionInstructions(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(12.dp))
        Text(message)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAction) { Text(actionLabel) }
    }
}
