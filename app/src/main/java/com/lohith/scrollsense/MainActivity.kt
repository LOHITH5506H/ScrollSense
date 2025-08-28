package com.lohith.scrollsense

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lohith.scrollsense.ui.UsageLogScreen
import com.lohith.scrollsense.ui.theme.ScrollSenseTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScrollSenseTheme {
                val ctx = this@MainActivity
                var permissionGranted by remember { mutableStateOf(false) }

                androidx.compose.material3.Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Use the innerPadding so the content doesn't overlap topbar / navbars
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        LaunchedEffect(Unit) {
                            delay(500) // small delay to let activity settle
                            if (UsageStatsHelper.isUsagePermissionGranted(ctx)) {
                                permissionGranted = true
                            } else {
                                Toast.makeText(
                                    ctx,
                                    "Please grant Usage Access permission",
                                    Toast.LENGTH_LONG
                                ).show()
                                UsageStatsHelper.openUsageSettings(ctx)
                            }
                        }

                        if (permissionGranted) {
                            UsageLogScreen()
                        }
                    }
                }
            }
        }
    }
}
// This is a comment