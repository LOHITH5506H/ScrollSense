package com.lohith.scrollsense.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lohith.scrollsense.util.PreferencesManager
import com.lohith.scrollsense.viewmodel.MainViewModel
import com.lohith.scrollsense.util.DataWiper
import com.lohith.scrollsense.workers.RetentionWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { PreferencesManager.get(ctx) }
    var retentionDays by remember { mutableStateOf(prefs.getRetentionDays()) }

    var parentPwdSet by remember { mutableStateOf(prefs.isParentPasswordSet()) }
    var parentPwd by remember { mutableStateOf("") }
    var parentPwdConfirm by remember { mutableStateOf("") }
    var pwdInput by remember { mutableStateOf("") }
    var pwdVerified by remember { mutableStateOf(false) }

    var pkgInput by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            // Clear Data
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Clear logs and data", fontWeight = FontWeight.SemiBold)
                Text("Removes usage logs, analytics, and locally stored files. This cannot be undone.")
                Button(onClick = {
                    viewModel.clearAllData()
                    DataWiper.wipeAppStorage(ctx)
                    onDismiss()
                }) { Text("Clear now") }
            }}

            // Auto deletion retention
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Auto-delete", fontWeight = FontWeight.SemiBold)
                Text("Choose how long to keep logs on device.")
                val options = listOf(15, 30, 90)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { d ->
                        FilterChip(selected = retentionDays == d, onClick = {
                            retentionDays = d
                            prefs.setRetentionDays(d)
                            // prune immediately and (re)schedule worker
                            viewModel.pruneOlderThan(d)
                            RetentionWorker.schedule(ctx)
                        }, label = { Text("$d days") })
                    }
                }
            }}

            // Parental controls
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Parental controls", fontWeight = FontWeight.SemiBold)
                if (!parentPwdSet) {
                    Text("Set a parent password to manage limits")
                    OutlinedTextField(value = parentPwd, onValueChange = { parentPwd = it }, label = { Text("Password") })
                    OutlinedTextField(value = parentPwdConfirm, onValueChange = { parentPwdConfirm = it }, label = { Text("Confirm password") })
                    Button(onClick = {
                        if (parentPwd.isNotBlank() && parentPwd == parentPwdConfirm) {
                            prefs.setParentPassword(parentPwd)
                            parentPwdSet = true
                        }
                    }) { Text("Set password") }
                } else if (!pwdVerified) {
                    Text("Enter parent password")
                    OutlinedTextField(value = pwdInput, onValueChange = { pwdInput = it }, label = { Text("Password") })
                    Button(onClick = {
                        pwdVerified = prefs.verifyParentPassword(pwdInput)
                    }) { Text("Verify") }
                } else {
                    Text("Add or update app limits (minutes per day)")
                    OutlinedTextField(value = pkgInput, onValueChange = { pkgInput = it }, label = { Text("Package name, e.g. com.whatsapp") })
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
            }}

            Spacer(Modifier.height(8.dp))
        }
    }
}

