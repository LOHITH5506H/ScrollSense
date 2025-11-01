package com.lohith.scrollsense.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lohith.scrollsense.util.AccessibilityServiceHelper
import com.lohith.scrollsense.util.PreferencesManager
import com.lohith.scrollsense.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var isServiceEnabled by remember {
        mutableStateOf(AccessibilityServiceHelper.isAccessibilityServiceEnabled(context))
    }
    var parentalControlEnabled by remember {
        mutableStateOf(preferencesManager.isParentalControlEnabled())
    }
    var pin by remember { mutableStateOf("") }
    var isUnlocked by remember {
        mutableStateOf(!parentalControlEnabled)
    }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var autoDeleteDays by remember {
        mutableStateOf(preferencesManager.getAutoDeleteDays())
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // This block handles the parental control lock logic
    if (parentalControlEnabled && !isUnlocked) {
        // --- Show Lock Screen ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter PIN", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (preferencesManager.checkParentalControlPin(pin)) {
                    isUnlocked = true
                    pin = "" // Clear pin
                } else {
                    // Show error
                }
            }) {
                Text("Unlock")
            }
        }
    } else {
        // --- Show Settings Screen ---
        // MODIFIED: Changed Column to LazyColumn to make it scrollable
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Accessibility Service
            item {
                SettingItem(
                    title = "Accessibility Service",
                    subtitle = if (isServiceEnabled) "Enabled" else "Disabled",
                    onClick = { openAccessibilitySettings(context) },
                    onCheckedChange = null
                )
            }

            // Parental Control
            item {
                SettingItem(
                    title = "Parental Controls",
                    subtitle = if (parentalControlEnabled) "Enabled" else "Disabled",
                    onClick = {
                        if (parentalControlEnabled) {
                            // Disable it (clear PIN)
                            preferencesManager.clearParentalControlPin()
                            parentalControlEnabled = false
                            isUnlocked = true
                        } else {
                            // Show dialog to set PIN
                            showSetPinDialog = true
                        }
                    },
                    onCheckedChange = {
                        if (it) {
                            showSetPinDialog = true
                        } else {
                            preferencesManager.clearParentalControlPin()
                            parentalControlEnabled = false
                            isUnlocked = true
                        }
                    }
                )
            }

            // Auto-delete
            item {
                Text("Auto-delete logs", modifier = Modifier.padding(top = 16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = autoDeleteDays == 7,
                        onClick = {
                            autoDeleteDays = 7
                            preferencesManager.setAutoDeleteDays(7)
                        }
                    )
                    Text("7 Days")
                    RadioButton(
                        selected = autoDeleteDays == 30,
                        onClick = {
                            autoDeleteDays = 30
                            preferencesManager.setAutoDeleteDays(30)
                        }
                    )
                    Text("30 Days")
                    RadioButton(
                        selected = autoDeleteDays == -1,
                        onClick = {
                            autoDeleteDays = -1
                            preferencesManager.setAutoDeleteDays(-1)
                        }
                    )
                    Text("Never")
                }
            }

            // Clear Logs
            item {
                Button(
                    onClick = { viewModel.clearAllLogs() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Clear All Logs")
                }
            }

            // Clear All Data
            item {
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Clear All Data (Wipe Everything)")
                }
            }

            // --- NEW: Backup and Restore Buttons ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Backup & Restore (For Testing)", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Button(
                    onClick = { viewModel.backupDatabase() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Backup Database to Downloads")
                }
            }

            item {
                Button(
                    onClick = { viewModel.restoreDatabase() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Restore Database from Downloads")
                }
            }
        }
    }

    // --- Dialogs ---
    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("Set Parental PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (pinError != null) {
                        Text(pinError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPin.length < 4) {
                        pinError = "PIN must be at least 4 digits."
                    } else if (newPin != confirmPin) {
                        pinError = "PINs do not match."
                    } else {
                        preferencesManager.setParentalControlPin(newPin)
                        parentalControlEnabled = true
                        isUnlocked = true
                        showSetPinDialog = false
                        newPin = ""
                        confirmPin = ""
                        pinError = null
                    }
                }) {
                    Text("Set")
                }
            },
            dismissButton = {
                Button(onClick = { showSetPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete all logs, analytics, and settings? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        preferencesManager.clearAll() // Also wipe preferences
                        parentalControlEnabled = false
                        isUnlocked = true
                        autoDeleteDays = -1
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onCheckedChange: ((Boolean) -> Unit)?
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            if (onCheckedChange != null) {
                Switch(
                    checked = subtitle == "Enabled",
                    onCheckedChange = onCheckedChange
                )
            }
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}