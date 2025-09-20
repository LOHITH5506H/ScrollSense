package com.lohith.scrollsense.ui.components

import android.graphics.Paint
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

@Composable
fun BarChart(
    data: Map<String, Long>,
    modifier: Modifier = Modifier,
    labelColor: Color = Color.Black
) {
    val colors = listOf(
        Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
        Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC)
    )

    val textMeasurer = rememberTextMeasurer()
    val maxValue = data.values.maxOrNull()?.toFloat() ?: 1f

    // Convert package names to readable app names
    val cleanedData = data.mapKeys { (key, _) ->
        when {
            key.contains("callui") || key.contains("dialer") -> "Call"
            key.contains("youtube") -> "YouTube"
            key.contains("amazon") -> "Amazon"
            key.contains("whatsapp") -> "WhatsApp"
            key.contains("chrome") -> "Chrome"
            key.contains("instagram") -> "Instagram"
            key.contains("facebook") -> "Facebook"
            key.contains("gmail") -> "Gmail"
            key.contains("maps") -> "Maps"
            key.contains("camera") -> "Camera"
            key.contains("gallery") -> "Gallery"
            key.contains("settings") -> "Settings"
            key.contains("contacts") -> "Contacts"
            key.contains("messages") || key.contains("sms") -> "Messages"
            key.contains("calculator") -> "Calculator"
            key.contains("calendar") -> "Calendar"
            key.contains("clock") -> "Clock"
            key.contains("music") -> "Music"
            key.contains("photos") -> "Photos"
            key.contains("play") -> "Play Store"
            key.contains("android.") -> key.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            key.contains(".") -> key.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            else -> key.replaceFirstChar { it.uppercase() }
        }
    }

    if (cleanedData.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No data to display")
        }
        return
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawBarChart(cleanedData, colors, maxValue, textMeasurer, labelColor)
    }
}

private fun DrawScope.drawBarChart(
    data: Map<String, Long>,
    colors: List<Color>,
    maxValue: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    labelColor: Color
) {
    val numBars = data.size
    val totalWidth = size.width
    val barWidth = totalWidth / (numBars * 1.8f) // Make bars wider
    val barSpacing = (totalWidth - (barWidth * numBars)) / (numBars + 1) // Even spacing
    val chartHeight = size.height * 0.6f // More space for chart
    val valueLabelOffset = 8f
    val bottomPadding = size.height * 0.3f // Space for labels

    // Paint object for rotated labels
    val paint = Paint().apply {
        isAntiAlias = true
        textSize = 12.sp.toPx() // Larger, more readable font
        textAlign = Paint.Align.CENTER
        color = labelColor.toArgb()
    }

    data.entries.forEachIndexed { index, (key, value) ->
        val barHeight = (value.toFloat() / maxValue) * chartHeight
        val x = barSpacing + index * (barWidth + barSpacing)
        val y = size.height - barHeight - bottomPadding

        // Draw bar with rounded corners for better appearance
        drawRect(
            color = colors[index % colors.size],
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight)
        )

        // Draw value on top of bar
        val timeInSeconds = value / 1000
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60

        val text = when {
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }

        val textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = labelColor)
        val textLayoutResult = textMeasurer.measure(text, textStyle)

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x + (barWidth - textLayoutResult.size.width) / 2,
                y - textLayoutResult.size.height - valueLabelOffset
            )
        )

        // Draw app name labels - positioned to avoid clipping
        val labelX = x + barWidth / 2
        val labelY = size.height - bottomPadding * 0.2f // Position near bottom but not clipped

        // Use shorter app names if too long
        val displayName = if (key.length > 8) {
            key.take(7) + "..."
        } else {
            key
        }

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(-45f, labelX, labelY)
        drawContext.canvas.nativeCanvas.drawText(displayName, labelX, labelY, paint)
        drawContext.canvas.nativeCanvas.restore()
    }
}
