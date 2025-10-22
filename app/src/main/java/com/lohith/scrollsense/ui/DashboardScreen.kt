package com.lohith.scrollsense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lohith.scrollsense.viewmodel.AppUsage
import com.lohith.scrollsense.viewmodel.MainViewModel

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    // Observe the StateFlow from the ViewModel
    val appUsageData by viewModel.appUsage.collectAsState()

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
                            AppUsageCard(app = app)
                        }
                    }
                }
            }
        }
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
                Text(formatDuration(totalTime))
            }
            Column {
                Text("Most Used", fontWeight = FontWeight.Bold)
                Text(mostUsed?.let { cleanAppName(it.appName) } ?: "N/A")
            }
        }
    }
}

// A card to display a single app's usage
@Composable
fun AppUsageCard(app: AppUsage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(cleanAppName(app.appName), fontWeight = FontWeight.Bold)
            Text("Usage: ${formatDuration(app.totalDuration)}")
        }
    }
}

private fun formatDuration(totalTimeInForeground: Long): String {
    val hours = totalTimeInForeground / 3_600_000
    val minutes = (totalTimeInForeground % 3_600_000) / 60_000
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalTimeInForeground / 1000}s"
    }
}
