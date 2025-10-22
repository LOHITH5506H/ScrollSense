package com.lohith.scrollsense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lohith.scrollsense.data.UsageEvent
import com.lohith.scrollsense.util.PackageNameHelper
import com.lohith.scrollsense.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit

@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val usageEvents by viewModel.usageEvents.collectAsState()

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
                    text = "Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF263238)
                )
                Spacer(Modifier.height(8.dp))
                if (usageEvents.isEmpty()) {
                    Text(
                        "No logs recorded yet. Start using other apps to see data here.",
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(usageEvents) { event ->
                            LogCard(event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogCard(event: UsageEvent) {
    // Get the current context to use for resolving the package name
    val context = LocalContext.current
    // Use the PackageNameHelper to get the user-friendly app name
    val appLabel = PackageNameHelper.getAppLabel(context, event.appLabel)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                // Display the resolved, user-friendly app name
                text = appLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = "Screen: ${event.screenTitle}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Category: ${event.category}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Duration: ${formatDuration(event.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Formats milliseconds into a human-readable "Xm Ys" format.
 */
private fun formatDuration(milliseconds: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

    return when {
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}