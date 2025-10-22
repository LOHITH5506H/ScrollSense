package com.lohith.scrollsense.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lohith.scrollsense.viewmodel.AiInsightsViewModelCompat
import java.util.concurrent.TimeUnit

class InsightsScreenCompat : Fragment() {

    private val vm: AiInsightsViewModelCompat by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    InsightsScreenContent(vm)
                }
            }
        }
    }
}

@Composable
private fun InsightsScreenContent(vm: AiInsightsViewModelCompat) {
    val insight by vm.insightText.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val error by vm.error.collectAsState()
    val canGenerate = vm.canGenerateNow()

    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(canGenerate) {
        if (!canGenerate) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                tick++
            }
        }
    }

    val millisLeft = if (canGenerate) 0 else vm.timeUntilNextWindowMillis()
    val hrs = TimeUnit.MILLISECONDS.toHours(millisLeft)
    val mins = TimeUnit.MILLISECONDS.toMinutes(millisLeft) % 60
    val secs = TimeUnit.MILLISECONDS.toSeconds(millisLeft) % 60

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "AI-Powered Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF263238)
                )
                if (insight.isBlank()) {
                    Text("No insight yet. Generate your first one for today.")
                } else {
                    Text(insight, color = Color(0xFF263238))
                }

                if (error != null) {
                    Text(error ?: "", color = Color(0xFFD32F2F), style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusText = if (!canGenerate) {
                        "Next refresh in %02d:%02d:%02d".format(hrs, mins, secs)
                    } else {
                        "You can generate a new response now."
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (canGenerate) Color(0xFF2E7D32) else Color(0xFF616161),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { vm.generateInsight() },
                        enabled = canGenerate && !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Generating…")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Generate")
                            Spacer(Modifier.width(8.dp))
                            Text("Generate")
                        }
                    }
                }

                Text(
                    text = "Insights combine today, last 7 days, and last 30 days of your usage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575)
                )
            }
        }
    }
}