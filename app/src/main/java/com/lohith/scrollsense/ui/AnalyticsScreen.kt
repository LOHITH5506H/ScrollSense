package com.lohith.scrollsense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lohith.scrollsense.ui.components.BarChart
import com.lohith.scrollsense.ui.components.PieChart
import com.lohith.scrollsense.viewmodel.MainViewModel

@Composable
fun AnalyticsScreen(viewModel: MainViewModel, paddingValues: PaddingValues) {
    // Collect data directly from the main ViewModel's flows
    val appUsage by viewModel.appUsage.collectAsState()
    val categoryUsage by viewModel.categoryUsage.collectAsState()

    // Convert data to the format required by the charts
    val appUsageDataMap = appUsage.associate { it.appName to it.totalDuration }
    val categoryUsageDataMap = categoryUsage.associate { it.categoryName to it.totalDuration }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Usage Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // App usage distribution Pie Chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Usage by App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    if (appUsageDataMap.isNotEmpty()) {
                        PieChart(
                            data = appUsageDataMap,
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    } else {
                        Text("No app usage data yet")
                    }
                }
            }
        }

        // Category breakdown Bar Chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Usage by Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    if (categoryUsageDataMap.isNotEmpty()) {
                        BarChart(
                            data = categoryUsageDataMap,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("No category data yet")
                    }
                }
            }
        }
    }
}