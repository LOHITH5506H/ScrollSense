package com.lohith.scrollsense

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lohith.scrollsense.ui.MainScreen
import com.lohith.scrollsense.ui.theme.ScrollSenseTheme
import com.lohith.scrollsense.util.PreferencesManager

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager(applicationContext)

        setContent {
            // ------------------------------------------------------------------
            // FIX: Added 'darkTheme = false' to force the light theme
            // ------------------------------------------------------------------
            ScrollSenseTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        preferencesManager = preferencesManager,
                        onNavigateToAccessibilitySettings = { navigateToAccessibilitySettings() },
                        onNavigateToUsageStatsSettings = { navigateToUsageStatsSettings() }
                    )
                }
            }
        }
    }

    private fun navigateToAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun navigateToUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }
}