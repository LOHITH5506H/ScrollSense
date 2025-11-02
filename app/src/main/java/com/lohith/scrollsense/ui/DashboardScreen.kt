package com.lohith.scrollsense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lohith.scrollsense.data.UsageEvent
import com.lohith.scrollsense.util.PackageNameHelper
import com.lohith.scrollsense.viewmodel.AppUsage
import com.lohith.scrollsense.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    // Observe the StateFlow from the ViewModel
    val appUsageData by viewModel.appUsage.collectAsState()
    val usageEvents by viewModel.usageEvents.collectAsState()

    // Observe the clicked app state
    val selectedAppPackage by viewModel.selectedAppPackage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Today's Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF263238)
                )
                Spacer(Modifier.height(8.dp))
                SummaryCard(appUsageData)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Most Used Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF263238)
                )
                Spacer(Modifier.height(8.dp))
                if (appUsageData.isEmpty()) {
                    Text("No usage data yet. Use your phone to see stats.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = appUsageData.take(5), key = { it.appName }) { app ->
                            AppUsageCard(
                                app = app,
                                onClick = {
                                    viewModel.onAppClicked(app.appName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Show the Modal Bottom Sheet when an app is selected
    if (selectedAppPackage != null) {
        val appName = cleanAppName(selectedAppPackage!!)
        val events = usageEventsForAppToday(usageEvents, selectedAppPackage!!)

        AppDetailsSheet(
            appName = appName,
            events = events,
            onDismiss = {
                viewModel.onAppClicked(null) // Dismiss the sheet
            }
        )
    }
}

// Add helper function for cleaning app names
private fun cleanAppName(appName: String): String {
    return when {
        appName.contains("callui") || appName.contains("dialer") -> "Call"
        appName.contains("youtube") -> "YouTube"
        appName.contains("amazon") -> "Amazon"
        appName.contains("whatsapp") -> "WhatsApp"
        appName.contains("chrome") -> "Chrome"
        appName.contains("instagram") -> "Instagram"
        appName.contains("facebook") -> "Facebook"
        appName.contains("gmail") -> "Gmail"
        appName.contains("maps") -> "Maps"
        appName.contains("camera") -> "Camera"
        appName.contains("gallery") -> "Gallery"
        appName.contains("settings") -> "Settings"
        appName.contains("contacts") -> "Contacts"
        appName.contains("messages") || appName.contains("sms") -> "Messages"
        appName.contains("calculator") -> "Calculator"
        appName.contains("calendar") -> "Calendar"
        appName.contains("clock") -> "Clock"
        appName.contains("music") -> "Music"
        appName.contains("photos") -> "Photos"
        appName.contains("play") -> "Play Store"
        appName.contains("android.") -> appName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        appName.contains(".") -> appName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        else -> appName.replaceFirstChar { it.uppercase() }
    }
}

// A simple card to display summary info
@Composable
fun SummaryCard(appUsageData: List<AppUsage>) {
    val totalTime = appUsageData.sumOf { it.totalDuration }
    val mostUsed = appUsageData.maxByOrNull { it.totalDuration }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column {
                Text("Screen Time", fontWeight = FontWeight.Bold)
                Text(formatDurationForSummary(totalTime))
            }
            Column {
                Text("Most Used", fontWeight = FontWeight.Bold)
                Text(mostUsed?.let { cleanAppName(it.appName) } ?: "N/A")
            }
        }
    }
}

private fun startOfDay(): Long {
    val c = java.util.Calendar.getInstance()
    c.set(java.util.Calendar.HOUR_OF_DAY, 0)
    c.set(java.util.Calendar.MINUTE, 0)
    c.set(java.util.Calendar.SECOND, 0)
    c.set(java.util.Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun usageEventsForAppToday(all: List<UsageEvent>, appLabel: String): List<UsageEvent> {
    val from = startOfDay()
    return all.asSequence()
        .filter { it.appLabel == appLabel && it.startTime >= from }
        .sortedByDescending { it.startTime }
        .toList()
}

@Composable
fun AppUsageCard(
    app: AppUsage,
    onClick: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(cleanAppName(app.appName), fontWeight = FontWeight.Bold)
            Text("Usage: ${formatDurationForSummary(app.totalDuration)}")
        }
    }
}

private fun formatDurationForSummary(totalTimeInForeground: Long): String {
    val hours = totalTimeInForeground / 3_600_000
    val minutes = (totalTimeInForeground % 3_600_000) / 60_000
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalTimeInForeground / 1000}s"
    }
}

// --- START: Sheet-specific Composables ---
// These are renamed to avoid conflicts with LogsScreen.kt

/**
 * This is the Composable for the Modal Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsSheet(
    appName: String,
    events: List<UsageEvent>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp) // Add padding for nav bar
        ) {
            // Title
            Text(
                text = appName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // List of Logs
            if (events.isEmpty()) {
                Text(
                    text = "No detailed logs found for this app today.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(events) { event ->
                        // RENAMED: Calling the unique composable
                        DashboardLogItem(event = event)
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}


/**
 * RENAMED: This is the log item, copied from LogsScreen.kt
 * but renamed to "DashboardLogItem" to avoid conflict.
 */
@Composable
fun DashboardLogItem(event: UsageEvent) {
    val context = LocalContext.current
    val appLabel = PackageNameHelper.getAppLabel(context, event.appLabel)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = appLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // MODIFIED: Use buildAnnotatedString to bold the label
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("Screen: ")
                    }
                    append(event.screenTitle)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            // MODIFIED: Use buildAnnotatedString to bold the label
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("Category: ")
                    }
                    append(event.category)
                },
                style = MaterialTheme.typography.bodySmall
            )

            // MODIFIED: Use buildAnnotatedString to bold the label
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("Duration: ")
                    }
                    append(formatDurationForDashboard(event.durationMs))
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * RENAMED: Copied from LogsScreen.kt and renamed to
 * "formatDurationForDashboard" to avoid conflict.
 */
private fun formatDurationForDashboard(milliseconds: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

    return when {
        minutes > 0 -> "${minutes}m ${seconds}s"
        seconds > 0 -> "${seconds}s"
        else -> "< 1s" // Handle very short durations
    }
}

// --- END: Sheet-specific Composables ---