package com.lohith.scrollsense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BarChart(
    data: Map<String, Long>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
        Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC)
    )

    val textMeasurer = rememberTextMeasurer()
    val maxValue = data.values.maxOrNull()?.toFloat() ?: 1f

    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No data to display")
        }
        return
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawBarChart(data, colors, maxValue, textMeasurer)
    }
}

private fun DrawScope.drawBarChart(
    data: Map<String, Long>,
    colors: List<Color>,
    maxValue: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val barWidth = size.width / (data.size * 1.5f)
    val barSpacing = barWidth * 0.5f
    val chartHeight = size.height * 0.8f

    data.entries.forEachIndexed { index, (key, value) ->
        val barHeight = (value.toFloat() / maxValue) * chartHeight
        val x = index * (barWidth + barSpacing) + barSpacing
        val y = size.height - barHeight - 40f // Leave space for labels

        // Draw bar
        drawRect(
            color = colors[index % colors.size],
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight)
        )

        // Draw value on top of bar
        val text = value.toString()
        val textSize = androidx.compose.ui.text.TextStyle(fontSize = 10.sp)
        val textLayoutResult = textMeasurer.measure(text, textSize)

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x + (barWidth - textLayoutResult.size.width) / 2,
                y - textLayoutResult.size.height - 4f
            )
        )

        // Draw category label
        val labelResult = textMeasurer.measure(
            key.take(8), // Truncate long labels
            androidx.compose.ui.text.TextStyle(fontSize = 8.sp)
        )
        drawText(
            textLayoutResult = labelResult,
            topLeft = Offset(
                x + (barWidth - labelResult.size.width) / 2,
                size.height - 30f
            )
        )
    }
}
