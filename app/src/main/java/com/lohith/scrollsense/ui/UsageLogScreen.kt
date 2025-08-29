// Enhanced UsageLogScreen.kt with Charts and Analytics
// Location: app/src/main/java/com/lohith/scrollsense/ui/UsageLogScreen.kt
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lohith.scrollsense.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background // added
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // added
import androidx.compose.material.icons.automirrored.filled.List // added
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lohith.scrollsense.data.UsageEvent
import com.lohith.scrollsense.ui.components.*
import com.lohith.scrollsense.viewmodel.UsageViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UsageLogScreen(viewModel: UsageViewModel = viewModel()) {
    val usageEvents by viewModel.usageEvents.collectAsState()
    val appUsageStats by viewModel.appUsageStats.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()
    var selectedView by remember { mutableStateOf("overview") }
    var selectedDateRange by remember { mutableStateOf("today") }
    var showClearDialog by remember { mutableStateOf(false) }

    // Update data based on selected date range
    LaunchedEffect(selectedDateRange) {
        viewModel.updateDateRange(selectedDateRange)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ScrollSense Analytics",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date range selector
                        DateRangeChip(
                            selectedRange = selectedDateRange,
                            onRangeSelected = { selectedDateRange = it }
                        )

                        // Clear logs button
                        IconButton(
                            onClick = { showClearDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear Logs",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        // Export button
                        IconButton(
                            onClick = { viewModel.exportData() }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Export Data"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // View selector tabs
                ViewSelectorTabs(
                    selectedView = selectedView,
                    onViewSelected = { selectedView = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content based on selected view
        when (selectedView) {
            "overview" -> OverviewContent(
                appUsageStats = appUsageStats,
                categoryStats = categoryStats,
                totalEvents = usageEvents.size,
                onAppClick = { app ->
                    selectedView = "details"
                    viewModel.selectApp(app)
                }
            )

            "charts" -> ChartsContent(
                appUsageStats = appUsageStats,
                categoryStats = categoryStats,
                onSegmentClick = { category ->
                    selectedView = "details"
                    viewModel.selectCategory(category)
                }
            )

            "details" -> DetailsContent(
                usageEvents = usageEvents,
                onBackClick = { selectedView = "overview" }
            )

            "logs" -> LogsContent(
                usageEvents = usageEvents
            )
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Logs") },
            text = { Text("This will permanently delete all usage data. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLogs()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DateRangeChip(
    selectedRange: String,
    onRangeSelected: (String) -> Unit
) {
    val ranges = listOf("today", "week", "month", "all")
    val rangeLabels = mapOf(
        "today" to "Today",
        "week" to "Week",
        "month" to "Month",
        "all" to "All"
    )

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        @Suppress("DEPRECATION")
        FilterChip(
            onClick = { expanded = true },
            label = { Text(rangeLabels[selectedRange] ?: "Today") },
            selected = true,
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            modifier = Modifier.menuAnchor() // suppressed
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ranges.forEach { range ->
                DropdownMenuItem(
                    text = { Text(rangeLabels[range] ?: range) },
                    onClick = {
                        onRangeSelected(range)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ViewSelectorTabs(
    selectedView: String,
    onViewSelected: (String) -> Unit
) {
    val tabs = listOf(
        "overview" to Icons.Default.Home,
        "charts" to Icons.Default.Info,
        "details" to Icons.AutoMirrored.Filled.List, // updated
        "logs" to Icons.Default.Settings
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { (view, icon) ->
            FilterChip(
                onClick = { onViewSelected(view) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = view.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            fontSize = 12.sp
                        )
                    }
                },
                selected = selectedView == view
            )
        }
    }
}

@Composable
fun OverviewContent(
    appUsageStats: Map<String, Long>,
    categoryStats: Map<String, Long>,
    totalEvents: Int,
    onAppClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Total Sessions",
                    value = totalEvents.toString(),
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Apps Used",
                    value = appUsageStats.size.toString(),
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Categories",
                    value = categoryStats.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top apps section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Most Used Apps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    appUsageStats.entries
                        .sortedByDescending { it.value }
                        .take(5)
                        .forEach { (app, duration) ->
                            AppUsageItem(
                                appName = app,
                                duration = duration,
                                percentage = (duration.toFloat() / appUsageStats.values.sum() * 100).toInt(),
                                onClick = { onAppClick(app) }
                            )
                        }
                }
            }
        }

        // Category breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Content Categories",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    categoryStats.entries
                        .sortedByDescending { it.value }
                        .forEach { (category, count) ->
                            CategoryItem(
                                category = category,
                                count = count.toInt(),
                                percentage = (count.toFloat() / categoryStats.values.sum() * 100).toInt()
                            )
                        }
                }
            }
        }
    }
}

@Composable
fun ChartsContent(
    appUsageStats: Map<String, Long>,
    categoryStats: Map<String, Long>,
    onSegmentClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App usage pie chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "App Usage Distribution",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (appUsageStats.isNotEmpty()) {
                        PieChart(
                            data = appUsageStats,
                            modifier = Modifier.fillMaxSize(),
                            onSegmentClick = onSegmentClick
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data available",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Category bar chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Category Breakdown",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryStats.isNotEmpty()) {
                        BarChart(
                            data = categoryStats,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data available",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsContent(
    usageEvents: List<UsageEvent>,
    onBackClick: () -> Unit
) {
    Column {
        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // updated
            }
            Text(
                text = "Detailed View",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Events list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(usageEvents.take(50)) { event ->
                UsageEventCard(event = event)
            }

            if (usageEvents.size > 50) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Showing first 50 events. Total: ${usageEvents.size}",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogsContent(usageEvents: List<UsageEvent>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(usageEvents.reversed()) { event ->
            UsageEventCard(event = event, compact = true)
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun AppUsageItem(
    appName: String,
    duration: Long,
    percentage: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDuration(duration)} • $percentage%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            LinearProgressIndicator(
                progress = { percentage / 100f }, // updated lambda overload
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: String,
    count: Int,
    percentage: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background( // now resolved
                        color = getCategoryColor(category),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "$count ($percentage%)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun UsageEventCard(
    event: UsageEvent,
    compact: Boolean = false
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.appLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = if (compact) 14.sp else 16.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = timeFormat.format(Date(event.startTime)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (!compact) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.screenTitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { },
                        label = { Text(text = event.category, fontSize = 10.sp) },
                        selected = false,
                        modifier = Modifier.height(24.dp)
                    )

                    if (event.durationMs > 0) {
                        Text(
                            text = formatDuration(event.durationMs),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}

private fun getCategoryColor(category: String): Color {
    val colors = listOf(
        Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
        Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC),
        Color(0xFFF06292), Color(0xFF90A4AE), Color(0xFFA1C181)
    )
    return colors[category.hashCode().rem(colors.size).let { if (it < 0) it + colors.size else it }]
}