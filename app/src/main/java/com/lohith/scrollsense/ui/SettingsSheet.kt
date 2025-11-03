package com.lohith.scrollsense.ui

import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
// --- NEW IMPORT ---
import androidx.compose.ui.text.input.PasswordVisualTransformation
// --- END NEW IMPORT ---
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.lohith.scrollsense.util.PreferencesManager
import com.lohith.scrollsense.viewmodel.MainViewModel
import com.lohith.scrollsense.util.DataWiper
import com.lohith.scrollsense.workers.RetentionWorker
import kotlinx.coroutines.launch
import com.lohith.scrollsense.ui.components.SettingsCategory
import com.lohith.scrollsense.ui.components.SettingsList
import com.lohith.scrollsense.ui.components.SwitchRow
import com.lohith.scrollsense.ui.components.ListRow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { PreferencesManager.get(ctx.applicationContext) }
    val scope = rememberCoroutineScope()
    var retentionDays by remember { mutableStateOf(prefs.getRetentionDays()) }

    var parentPwdSet by remember { mutableStateOf(prefs.isParentPasswordSet()) }
    var parentPwd by remember { mutableStateOf("") }
    var parentPwdConfirm by remember { mutableStateOf("") }
    var pwdInput by remember { mutableStateOf("") }
    var pwdVerified by remember { mutableStateOf(false) }

    val installedApps by viewModel.installedApps.collectAsState()
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedAppText by remember { mutableStateOf("") }
    var selectedAppPackage by remember { mutableStateOf("") }
    var textFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var minutesInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    val dropdownIcon = if (isDropdownExpanded)
        Icons.Filled.ArrowDropUp
    else
        Icons.Filled.ArrowDropDown

    // Create a sheet state that skips the 50% "partially expanded" state
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        // These modifiers handle the keyboard scrolling
        Column(
            Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            SettingsCategory(title = { Text("Data Management", fontWeight = FontWeight.Bold) }) {
                ListRow(
                    title = { Text("Clear All Logs") },
                    subtitle = { Text("Deletes all recorded usage data") },
                    onClick = {
                        scope.launch {
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

                        // --- THIS IS THE FIX ---
                        OutlinedTextField(
                            value = parentPwd,
                            onValueChange = { parentPwd = it },
                            label = { Text("New Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        OutlinedTextField(
                            value = parentPwdConfirm,
                            onValueChange = { parentPwdConfirm = it },
                            label = { Text("Confirm Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        // --- END FIX ---

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

                        // --- THIS IS THE FIX ---
                        OutlinedTextField(
                            value = pwdInput,
                            onValueChange = { pwdInput = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        // --- END FIX ---

                        Button(onClick = {
                            if (prefs.verifyParentPassword(pwdInput)) {
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
                        Text("App Time Limits")

                        // --- Refactored Dropdown ---
                        Box {
                            OutlinedTextField(
                                value = selectedAppText,
                                onValueChange = {
                                    selectedAppText = it
                                    isDropdownExpanded = true
                                },
                                label = { Text("Select an App") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        textFieldSize = coordinates.size.toSize()
                                    },
                                trailingIcon = {
                                    Icon(
                                        imageVector = dropdownIcon,
                                        contentDescription = "Open dropdown",
                                        modifier = Modifier.clickable { isDropdownExpanded = !isDropdownExpanded }
                                    )
                                }
                            )

                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false },
                                modifier = Modifier
                                    .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                            ) {
                                val filteredApps = installedApps.filter {
                                    it.appName.contains(selectedAppText, ignoreCase = true) ||
                                            it.packageName.contains(selectedAppText, ignoreCase = true)
                                }

                                if (filteredApps.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No apps found") },
                                        onClick = { }
                                    )
                                }

                                filteredApps.forEach { app ->
                                    DropdownMenuItem(
                                        text = { Text(app.appName) },
                                        onClick = {
                                            selectedAppText = app.appName
                                            selectedAppPackage = app.packageName
                                            isDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = minutesInput,
                            onValueChange = { minutesInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Minutes per day") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val min = minutesInput.toIntOrNull() ?: 0
                                if (selectedAppPackage.isNotBlank() && min > 0) {
                                    prefs.setParentalLimit(selectedAppPackage, min)
                                    selectedAppText = ""
                                    selectedAppPackage = ""
                                    minutesInput = ""
                                    Toast.makeText(ctx, "Limit saved", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(ctx, "Please select an app and set minutes", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("Save limit") }

                            OutlinedButton(onClick = {
                                if (selectedAppPackage.isNotBlank()) {
                                    prefs.removeParentalLimit(selectedAppPackage)
                                    selectedAppText = ""
                                    selectedAppPackage = ""
                                    minutesInput = ""
                                    Toast.makeText(ctx, "Limit removed", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(ctx, "Please select an app to remove", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("Remove limit") }
                        }

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
            Spacer(Modifier.height(8.dp)) // Extra space at the bottom
        }
    }
}