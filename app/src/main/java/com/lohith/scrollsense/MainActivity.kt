package com.lohith.scrollsense

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lohith.scrollsense.ui.MainScreen
import com.lohith.scrollsense.ui.theme.ScrollSenseTheme
import com.lohith.scrollsense.viewmodel.MainViewModel
import com.lohith.scrollsense.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    // --- ADD THIS LAUNCHER ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // You can handle granted/denied permissions here if needed
        permissions.entries.forEach {
            Log.d("MainActivity", "${it.key} = ${it.value}")
        }
    }

    // --- ADD THIS FUNCTION ---
    private fun checkAndRequestStoragePermission() {
        val permissionsToRequest = mutableListOf<String>()

        // For Android 12 (API 32) and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        // For Android 13 (API 33) and above, permissions are handled differently
        // and a general file write to Downloads is more complex, but
        // READ_EXTERNAL_STORAGE might still be needed if targeting older SDKs.
        // This simple legacy request should cover your backup goal.

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- ADD THIS LINE ---
        checkAndRequestStoragePermission() // Request permissions on startup

        setContent {
            ScrollSenseTheme {
                val viewModel: MainViewModel = viewModel(factory = ViewModelFactory(application))
                MainScreen(viewModel = viewModel)
            }
        }
    }
}