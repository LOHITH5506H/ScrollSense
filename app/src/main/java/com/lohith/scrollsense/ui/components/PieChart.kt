package com.lohith.scrollsense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

private val PieColors = listOf(
    Color(0xFFF4C430), // Saffron
    Color(0xFF4DB6AC),
    Color(0xFF64B5F6),
    Color(0xFFFF8A65),
    Color(0xFF9575CD),
    Color(0xFF81C784),
    Color(0xFFE57373),
    Color(0xFF90A4AE)
)

@Composable
fun PieChart(
    data: Map<String, Long>,
    modifier: Modifier = Modifier,
    ringThickness: Dp = 40.dp,
    centerLabel: String = "Content Usage",
    labelColor: Color = Color.Black,
    onSegmentClick: (String) -> Unit = {},
    showLegendInside: Boolean = false,
    drawSliceLabels: Boolean = false,
    orderedNames: List<String>? = null
) {
    val total = data.values.sum().toFloat()

    if (total == 0f || data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) { Text("No data to display") }
        return
    }

    var selectedSegment by remember { mutableStateOf<String?>(null) }
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerRadius = min(size.width, size.height) / 2f * 0.90f
                        val innerRadius = outerRadius - ringThickness.toPx()
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance in innerRadius..outerRadius) {
                            val angle = (atan2(dy, dx) * 180f / PI.toFloat()).let { if (it < 0) it + 360f else it }
                            var currentAngle = 0f
                            for ((key, value) in data) {
                                val sweepAngle = (value / total) * 360f
                                if (angle >= currentAngle && angle <= currentAngle + sweepAngle) {
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
            drawDonutChart(
                data = data,
                colors = PieColors,
                total = total,
                ringThicknessPx = ringThickness.toPx(),
                selectedSegment = selectedSegment,
                textMeasurer = textMeasurer,
                labelColor = labelColor,
                drawSliceLabels = drawSliceLabels,
                orderedNames = orderedNames
            )
        }

        // Center label
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleLarge,
                color = labelColor,
                textAlign = TextAlign.Center
            )
        }

        if (showLegendInside) {
            val totalLegend = data.values.sum().coerceAtLeast(1L).toFloat()
            val sorted = data.entries.sortedByDescending { it.value }.take(5)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sorted.forEachIndexed { index, (name, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(PieColors[index % PieColors.size], shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${((value / totalLegend) * 100).toInt()}% $name",
                            color = labelColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PieLegend(
    data: List<Pair<String, Long>>, // in the same order used to draw the chart
    modifier: Modifier = Modifier,
    labelColor: Color = Color.Black,
    maxItems: Int = min(8, data.size)
) {
    val total = data.sumOf { it.second }.coerceAtLeast(1L).toFloat()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.take(maxItems).forEachIndexed { index, (name, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(PieColors[index % PieColors.size], shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${((value / total) * 100).toInt()}% $name",
                    color = labelColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun DrawScope.drawDonutChart(
    data: Map<String, Long>,
    colors: List<Color>,
    total: Float,
    ringThicknessPx: Float,
    selectedSegment: String?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    labelColor: Color,
    drawSliceLabels: Boolean,
    orderedNames: List<String>? = null
) {
    val outerRadius = kotlin.math.min(size.width, size.height) / 2f * 0.90f
    val innerRadius = outerRadius - ringThicknessPx
    val center = Offset(size.width / 2f, size.height / 2f)

    var startAngle = -90f

    // Build the iteration list in the exact order requested
    val entries: List<Pair<String, Long>> = if (orderedNames != null) {
        orderedNames.map { it to (data[it] ?: 0L) }
    } else data.entries.map { it.key to it.value }

    // Decide which slices get labels: top 2 + any >= 15%
    val sortedByValue = entries.sortedByDescending { it.second }
    val topNames = sortedByValue.take(2).map { it.first }.toSet()

    entries.forEachIndexed { index, (key, value) ->
        val sweepAngle = (value.toFloat() / total) * 360f
        if (sweepAngle <= 0f) return@forEachIndexed
        val isSelected = key == selectedSegment
        val arcRadius = if (isSelected) outerRadius * 1.03f else outerRadius
        val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
        val arcSize = Size(arcRadius * 2, arcRadius * 2)

        drawArc(
            color = colors[index % colors.size],
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = ringThicknessPx, cap = StrokeCap.Butt)
        )

        // Label logic
        val pct = (value.toFloat() / total * 100f)
        val shouldLabel = drawSliceLabels && (pct >= 15f || key in topNames)
        if (shouldLabel) {
            val midAngleRad = (startAngle + sweepAngle / 2f) * (Math.PI.toFloat() / 180f)
            val labelRadius = innerRadius + (ringThicknessPx * 0.55f)
            var lx = center.x + labelRadius * kotlin.math.cos(midAngleRad)
            var ly = center.y + labelRadius * kotlin.math.sin(midAngleRad)

            val pctInt = pct.roundToInt()
            val text = if (pctInt >= 10) "$pctInt % ${key.take(14)}" else "$pctInt %"
            val layout = textMeasurer.measure(
                text = text,
                style = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = labelColor)
            )

            val margin = 12f
            lx = lx.coerceIn(margin, size.width - layout.size.width - margin)
            ly = ly.coerceIn(margin, size.height - layout.size.height - margin)

            drawText(
                textLayoutResult = layout,
                topLeft = Offset(lx, ly)
            )
        }

        startAngle += sweepAngle
    }

    // Inner hole
    drawCircle(
        color = Color.Transparent,
        radius = innerRadius,
        center = center,
        style = Stroke(width = 0f)
    )
}
