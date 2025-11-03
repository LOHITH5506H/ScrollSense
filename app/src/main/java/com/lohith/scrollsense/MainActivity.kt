package com.lohith.scrollsense

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lohith.scrollsense.ui.MainScreen
import com.lohith.scrollsense.ui.theme.ScrollSenseTheme
import com.lohith.scrollsense.util.AccessibilityServiceHelper
import com.lohith.scrollsense.util.UsageStatsHelper
import com.lohith.scrollsense.util.PreferencesManager
import com.lohith.scrollsense.workers.AnalyticsWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "Notification permission granted")
            } else {
                Log.w("MainActivity", "Notification permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager.get(applicationContext)

        checkAndRequestPermissions()

        // Schedule the periodic worker
        scheduleAnalyticsWorker()

        setContent {
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

    // --- Function to schedule the worker ---
    private fun scheduleAnalyticsWorker() {
        // --- THIS IS THE FIX ---
        // Changed from 30 to 15, the minimum allowed time for a periodic worker.
        val analyticsWorkRequest =
            PeriodicWorkRequestBuilder<AnalyticsWorker>(15, TimeUnit.MINUTES)
                .build()
        // --- END FIX ---

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AnalyticsWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep the existing work if it's already scheduled
            analyticsWorkRequest
        )
        Log.d("MainActivity", "AnalyticsWorker scheduled to run every 15 minutes.")
    }

    private fun checkAndRequestPermissions() {
        if (!UsageStatsHelper.isUsagePermissionGranted(this)) {
            navigateToUsageStatsSettings()
        }
        if (!AccessibilityServiceHelper.isServiceEnabled(this)) {
            navigateToAccessibilitySettings()
        }
        askForNotificationPermission()
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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