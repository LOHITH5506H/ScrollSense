package com.lohith.scrollsense.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lohith.scrollsense.viewmodel.ChartViewModel
import com.lohith.scrollsense.ui.components.PieChart
import com.lohith.scrollsense.ui.components.BarChart
import com.lohith.scrollsense.ui.components.StackedBarChart

/**
 * Dedicated analytics screen (separate from UsageLogScreen) that focuses purely on
 * visual chart representations of usage data.
 */
@Composable
fun AnalyticsScreen(
    viewModel: ChartViewModel = viewModel()
) {
    val appUsage by viewModel.appUsage.collectAsState()
    val categoryUsage by viewModel.categoryUsage.collectAsState()
    val appCategory by viewModel.appCategoryDurations.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analytics Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // App usage distribution
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                colors = CardDefaults.cardColors()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "App Usage Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    if (appUsage.isNotEmpty()) {
                        PieChart(
                            data = appUsage,
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    } else {
                        Text("No app usage data yet")
                    }
                }
            }
        }

        // Category breakdown (counts)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    if (categoryUsage.isNotEmpty()) {
                        BarChart(
                            data = categoryUsage,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("No category data yet")
                    }
                }
            }
        }

        // Stacked per-app by category (duration)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Time by App and Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    StackedBarChart(
                        rows = appCategory,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
