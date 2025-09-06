package com.lohith.scrollsense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.lohith.scrollsense.data.AppCategoryDuration

@Composable
fun StackedBarChart(
    rows: List<AppCategoryDuration>,
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data to display")
        }
        return
    }

    // Distinct apps and categories
    val apps = rows.map { it.appLabel }.distinct()
    val categories = rows.map { it.category }.distinct()

    // Color palette
    val colors = listOf(
        Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
        Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC),
        Color(0xFFF06292), Color(0xFF90A4AE), Color(0xFFA1C181)
    )

    // Precompute totals per app
    val totalsByApp: Map<String, Long> = apps.associateWith { app ->
        rows.filter { it.appLabel == app }.sumOf { it.totalDuration }
    }

    val maxTotal = (totalsByApp.values.maxOrNull() ?: 1L).toFloat()
    val textMeasurer = rememberTextMeasurer()
    // Capture theme color outside Canvas (can't call @Composable inside DrawScope)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.fillMaxSize()) {
        val barWidth = size.width / (apps.size * 1.5f)
        val barSpacing = barWidth * 0.5f
        val chartHeight = size.height * 0.8f

        apps.forEachIndexed { index, app ->
            val x = index * (barWidth + barSpacing) + barSpacing
            val yBase = size.height - 40f

            // Stack segments for this app
            val appRows = rows.filter { it.appLabel == app }
            var accumulated = 0f

            categories.forEachIndexed { cIndex, cat ->
                val value = appRows.firstOrNull { it.category == cat }?.totalDuration ?: 0L
                if (value <= 0L) return@forEachIndexed
                val height = (value.toFloat() / maxTotal) * chartHeight
                val top = yBase - (accumulated + height)

                drawRect(
                    color = colors[cIndex % colors.size],
                    topLeft = Offset(x, top),
                    size = Size(barWidth, height)
                )

                accumulated += height
            }

            // Draw app label
            val labelResult = textMeasurer.measure(
                text = app.take(8),
                style = androidx.compose.ui.text.TextStyle(fontSize = 8.sp, color = onSurfaceColor)
            )
            drawText(
                textLayoutResult = labelResult,
                topLeft = Offset(
                    x + (barWidth - labelResult.size.width) / 2,
                    yBase + 4f
                )
            )
        }
    }
}
