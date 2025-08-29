package com.lohith.scrollsense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun PieChart(
    data: Map<String, Long>,
    modifier: Modifier = Modifier,
    onSegmentClick: (String) -> Unit = {}
) {
    val colors = listOf(
        Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
        Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC),
        Color(0xFFF06292), Color(0xFF90A4AE), Color(0xFFA1C181)
    )

    val total = data.values.sum().toFloat()

    if (total == 0f) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No data to display")
        }
        return
    }

    var selectedSegment by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = minOf(size.width, size.height) / 2f * 0.8f
                        val distance = sqrt((offset.x - center.x).pow(2) + (offset.y - center.y).pow(2))

                        if (distance <= radius) {
                            val angle = atan2(offset.y - center.y, offset.x - center.x) * 180 / PI
                            val normalizedAngle = if (angle < 0) angle + 360 else angle

                            var currentAngle = 0f
                            for ((key, value) in data) {
                                val sweepAngle = (value / total) * 360f
                                if (normalizedAngle >= currentAngle && normalizedAngle <= currentAngle + sweepAngle) {
                                    selectedSegment = key
                                    onSegmentClick(key)
                                    break
                                }
                                currentAngle += sweepAngle
                            }
                        }
                    }
                }
        ) {
            drawPieChart(data, colors, total, selectedSegment)
        }

        // Legend
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            data.entries.take(5).forEachIndexed { index, (key, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                colors[index % colors.size],
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$key (${(value.toFloat() / total * 100).toInt()}%)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawPieChart(
    data: Map<String, Long>,
    colors: List<Color>,
    total: Float,
    selectedSegment: String?
) {
    val radius = minOf(size.width, size.height) / 2f * 0.8f
    val center = size.center

    var startAngle = 0f

    data.entries.forEachIndexed { index, (key, value) ->
        val sweepAngle = (value.toFloat() / total) * 360f
        val color = colors[index % colors.size]
        val isSelected = key == selectedSegment
        val currentRadius = if (isSelected) radius * 1.05f else radius

        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(
                center.x - currentRadius,
                center.y - currentRadius
            ),
            size = androidx.compose.ui.geometry.Size(
                currentRadius * 2,
                currentRadius * 2
            )
        )

        startAngle += sweepAngle
    }
}
