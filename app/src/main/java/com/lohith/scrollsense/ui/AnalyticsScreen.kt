package com.lohith.scrollsense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lohith.scrollsense.ui.components.BarChart
import com.lohith.scrollsense.ui.components.PieChart
import com.lohith.scrollsense.ui.components.PieLegend
import com.lohith.scrollsense.util.PackageNameHelper
import com.lohith.scrollsense.viewmodel.MainViewModel

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val categoryData by viewModel.categoryUsage.collectAsState()
    val appUsage by viewModel.appUsage.collectAsState()
    val context = LocalContext.current

    // Normalize category name (trim & lowercase) and merge all "other" buckets
    fun normalize(name: String) = name.trim().lowercase()

    // Sum by normalized category
    val summedByNorm: Map<String, Long> = buildMap<String, Long> {
        categoryData.forEach { c ->
            val key = normalize(c.categoryName)
            val prev = get(key) ?: 0L
            put(key, prev + c.totalDuration)
        }
    }

    val otherDuration = summedByNorm["other"] ?: 0L
    // Build display pairs for non-other categories
    val nonOtherPairs = summedByNorm
        .filterKeys { it != "other" }
        .map { (norm, dur) ->
            // Title-case the first letter for display
            val display = norm.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            display to dur
        }
        .sortedByDescending { it.second }

    // Take top 7 non-other categories to include new ones like Fitness and Adult
    val topNonOther = nonOtherPairs.take(7)
    val remainderNonOther = nonOtherPairs.drop(7).sumOf { it.second }
    val mergedOther = otherDuration + remainderNonOther

    // Final ordered list for the pie chart
    val orderedPairs = buildList {
        if (mergedOther > 0) add("Other" to mergedOther)
        addAll(topNonOther)
    }

    val categoryMap = linkedMapOf<String, Long>().apply { orderedPairs.forEach { put(it.first, it.second) } }

    val topApps = appUsage.sortedByDescending { it.totalDuration }.take(5)
    // Resolve package names to app labels for the bar chart
    val appMap = topApps.associate {
        // it.appName from the ViewModel is actually the packageName
        val appName = PackageNameHelper.getAppLabel(context, it.appName)
        appName to it.totalDuration
    }

    if (categoryMap.isEmpty() && appMap.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text("No category data to analyze.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category distribution pie chart card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Usage by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF263238)
                )
                Spacer(Modifier.height(8.dp))
                PieChart(
                    data = categoryMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    ringThickness = 56.dp,
                    centerLabel = "Content Usage",
                    showLegendInside = false,
                    drawSliceLabels = false
                )
                Spacer(Modifier.height(12.dp))
                // Dedicated legend under the chart
                PieLegend(
                    data = orderedPairs,
                    modifier = Modifier.fillMaxWidth(),
                    labelColor = Color.Black
                )
            }
        }

        // Top apps bar chart card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Top Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF263238)
                )
                Spacer(Modifier.height(8.dp))
                BarChart(
                    data = appMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    labelColor = Color.Black // Explicitly set label color
                )
            }
        }
    }
}

