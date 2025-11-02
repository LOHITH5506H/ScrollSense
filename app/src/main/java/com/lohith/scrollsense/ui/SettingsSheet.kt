package com.lohith.scrollsense.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lohith.scrollsense.util.PreferencesManager
import com.lohith.scrollsense.viewmodel.MainViewModel
import com.lohith.scrollsense.util.DataWiper // We still need this for the *other* wipe function (wipeAppStorage)
import com.lohith.scrollsense.workers.RetentionWorker
import kotlinx.coroutines.launch

// These imports should now be correct as they point to your local components
import com.lohith.scrollsense.ui.components.SettingsCategory
import com.lohith.scrollsense.ui.components.SettingsList
import com.lohith.scrollsense.ui.components.SwitchRow
import com.lohith.scrollsense.ui.components.ListRow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { PreferencesManager(ctx.applicationContext) }
    val scope = rememberCoroutineScope()
    var retentionDays by remember { mutableStateOf(prefs.getRetentionDays()) }

    var parentPwdSet by remember { mutableStateOf(prefs.isParentPasswordSet()) }
    var parentPwd by remember { mutableStateOf("") }
    var parentPwdConfirm by remember { mutableStateOf("") }
    var pwdInput by remember { mutableStateOf("") }
    var pwdVerified by remember { mutableStateOf(false) }

    var pkgInput by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            SettingsCategory(title = { Text("Data Management", fontWeight = FontWeight.Bold) }) {
                ListRow(
                    title = { Text("Clear All Logs") },
                    subtitle = { Text("Deletes all recorded usage data") },
                    onClick = {
                        scope.launch {
                            // ------------------------------------------------------------------
                            // FIX: Call the ViewModel's 'clearAllData' function.
                            // This will clear the DB and the UI will update automatically.
                            // ------------------------------------------------------------------
                            viewModel.clearAllData()
                            Toast.makeText(ctx, "All data cleared", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                )

                SettingsList<String>(
                    title = { Text("Auto Delete Logs") },
                    value = retentionDays.toString(),
                    items = listOf("0", "15", "30"),
                    display = {
                        when (it) {
                            "0" -> "Never"
                            "15" -> "After 15 days"
                            "30" -> "After 30 days"
                            else -> "Never"
                        }
                    },
                    onValueChange = {
                        val days = it.toIntOrNull() ?: 0
                        retentionDays = days
                        prefs.setRetentionDays(days)
                        RetentionWorker.schedule(ctx)
                    }
                )
            }

            SettingsCategory(title = { Text("Parental Controls", fontWeight = FontWeight.Bold) }) {
                if (!parentPwdSet) {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Set a password to enable parental controls.")
                        OutlinedTextField(value = parentPwd, onValueChange = { parentPwd = it }, label = { Text("New Password") })
                        OutlinedTextField(value = parentPwdConfirm, onValueChange = { parentPwdConfirm = it }, label = { Text("Confirm Password") })
                        Button(onClick = {
                            if (parentPwd.isNotBlank() && parentPwd == parentPwdConfirm) {
                                prefs.setParentPassword(parentPwd)
                                parentPwdSet = true
                                pwdVerified = true
                                parentPwd = ""; parentPwdConfirm = ""
                            } else {
                                Toast.makeText(ctx, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Set Password") }
                    }
                } else if (!pwdVerified) {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter password to manage parental controls.")
                        OutlinedTextField(value = pwdInput, onValueChange = { pwdInput = it }, label = { Text("Password") })
                        Button(onClick = {
                            if (prefs.getParentPassword() == pwdInput) {
                                pwdVerified = true
                                pwdInput = ""
                            } else {
                                Toast.makeText(ctx, "Incorrect password", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Unlock") }
                    }
                } else {
                    // Controls are unlocked
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("App Time Limits (Package name & Minutes)")
                        OutlinedTextField(value = pkgInput, onValueChange = { pkgInput = it }, label = { Text("com.example.app") })
                        OutlinedTextField(value = minutesInput, onValueChange = { minutesInput = it.filter { ch -> ch.isDigit() } }, label = { Text("Minutes") })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val min = minutesInput.toIntOrNull() ?: 0
                                if (pkgInput.isNotBlank() && min > 0) {
                                    prefs.setParentalLimit(pkgInput.trim(), min)
                                    pkgInput = ""; minutesInput = ""
                                }
                            }) { Text("Save limit") }
                            OutlinedButton(onClick = {
                                if (pkgInput.isNotBlank()) {
                                    prefs.removeParentalLimit(pkgInput.trim())
                                    pkgInput = ""
                                }
                            }) { Text("Remove limit") }
                        }
                        // Show current limits summary
                        val limits = prefs.getParentalLimits()
                        if (limits.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                limits.forEach { (pkg, m) -> Text("$pkg: $m min/day") }
                            }
                        } else {
                            Text("No limits set yet.")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}