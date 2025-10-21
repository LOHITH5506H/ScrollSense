package com.lohith.scrollsense.export

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.lohith.scrollsense.analytics.WeeklyAnalytics
import com.lohith.scrollsense.data.DailySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * PDF Export system for usage analytics
 * Generates comprehensive PDF reports with charts and data tables
 */
class PDFExporter(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595  // A4 width in points (72 dpi)
        private const val PAGE_HEIGHT = 842 // A4 height in points (72 dpi)
        private const val MARGIN = 40
        private const val CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN)
        private const val FOOTER_HEIGHT = 24
        private const val MIN_APP_ROW_MS = 60_000L
    }

    // Typography
    private val titlePaint = Paint().apply {
        textSize = 26f
        color = Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val headerPaint = Paint().apply {
        textSize = 16f
        color = Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val subHeaderPaint = Paint().apply {
        textSize = 12.5f
        color = Color.DKGRAY
        isAntiAlias = true
    }

    private val bodyPaint = Paint().apply {
        textSize = 12f
        color = Color.BLACK
        isAntiAlias = true
    }

    private val faintPaint = Paint().apply {
        textSize = 11f
        color = Color.rgb(150,150,150)
        isAntiAlias = true
    }

    private val rulePaint = Paint().apply {
        color = Color.rgb(220, 220, 220)
        strokeWidth = 1.2f
        isAntiAlias = true
    }

    private val chartPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // ---------- Public API ----------

    /**
     * Export daily report as PDF
     */
    suspend fun exportDailyReport(
        dailySummary: DailySummary,
        categoryAnalytics: List<com.lohith.scrollsense.data.DailyCategoryAnalytics>,
        appAnalytics: List<com.lohith.scrollsense.data.DailyAppAnalytics>
    ): File = withContext(Dispatchers.IO) {

        val document = PdfDocument()

        // Simple page management utilities
        var pageIndex = 0
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = 0f

        fun footer() {
            val footerText = "ScrollSense • ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())} • Page $pageIndex"
            val yFooter = PAGE_HEIGHT - (FOOTER_HEIGHT / 2f)
            canvas.drawLine(MARGIN.toFloat(), yFooter - 10f, (PAGE_WIDTH - MARGIN).toFloat(), yFooter - 10f, rulePaint)
            canvas.drawText(footerText, MARGIN.toFloat(), yFooter, faintPaint)
        }

        fun startPage(title: String, subtitle: String) {
            pageIndex += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
            page = document.startPage(info)
            canvas = page.canvas
            // Header
            var headerY = MARGIN + 6f
            canvas.drawText(title, MARGIN.toFloat(), headerY + titlePaint.textSize, titlePaint)
            headerY += titlePaint.textSize + 6f
            canvas.drawText(subtitle, MARGIN.toFloat(), headerY + subHeaderPaint.textSize, subHeaderPaint)
            headerY += subHeaderPaint.textSize + 10f
            canvas.drawLine(MARGIN.toFloat(), headerY, (PAGE_WIDTH - MARGIN).toFloat(), headerY, rulePaint)
            y = headerY + 18f
        }

        fun finishPageIfNeeded() {
            footer()
            document.finishPage(page)
        }

        fun ensureSpace(required: Float, title: String, subtitle: String) {
            val bottomLimit = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT
            if (y + required > bottomLimit) {
                finishPageIfNeeded()
                startPage(title, subtitle)
            }
        }

        val title = "ScrollSense Daily Report"
        val subtitle = "Date: ${dailySummary.date}"
        startPage(title, subtitle)

        // Summary KPIs — concise and sanitized
        canvas.drawText("Daily Summary", MARGIN.toFloat(), y, headerPaint)
        y += 22f
        val k1 = "Total Screen Time: ${formatDuration(dailySummary.totalScreenTimeMs)}"
        val k2 = "Number of Sessions: ${dailySummary.sessionsCount}"
        val k3 = "Average Session: ${formatDuration(dailySummary.averageSessionDurationMs)}"
        val topAppClean = sanitizeAppName(dailySummary.topApp ?: "-", null).replaceFirstChar { it.uppercase() }
        val k4 = "Most Used App: ${if (topAppClean.isBlank()) "-" else topAppClean}"
        val stats = listOf(k1, k2, k3, k4)
        val lineHeight = 16f
        stats.chunked(2).forEach { pair ->
            ensureSpace(lineHeight + 2f, title, subtitle)
            canvas.drawText(pair[0], MARGIN.toFloat(), y, bodyPaint)
            if (pair.size > 1) canvas.drawText(pair[1], (MARGIN + CONTENT_WIDTH / 2f).toFloat(), y, bodyPaint)
            y += lineHeight
        }

        y += 12f
        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, rulePaint)
        y += 16f

        // Category breakdown — Top 5 + Others
        canvas.drawText("Category Breakdown", MARGIN.toFloat(), y, headerPaint)
        y += 18f
        val pieSize = 170f
        ensureSpace(pieSize + 18f, title, subtitle)
        val catSorted = categoryAnalytics.sortedByDescending { it.totalTimeMs }
        val (topCats, others) = if (catSorted.size > 5) catSorted.take(5) to catSorted.drop(5) else catSorted to emptyList()
        val othersMs = others.sumOf { it.totalTimeMs }
        val pieData = buildList {
            topCats.forEach { add(ChartData(it.category, it.totalTimeMs.toFloat())) }
            if (othersMs > 0) add(ChartData("Others", othersMs.toFloat()))
        }
        val chartRect = RectF(MARGIN.toFloat(), y, MARGIN + pieSize, y + pieSize)
        drawPieChart(canvas, chartRect, pieData)

        // Legend right side — compact
        val legendXStart = MARGIN + pieSize + 14f
        var legendY = y + 8f
        pieData.forEachIndexed { idx, c ->
            chartPaint.color = getChartColor(idx)
            canvas.drawCircle(legendXStart, legendY - 4f, 5f, chartPaint)
            val pct = pieData.sumOf { it.value.toDouble() }.toFloat().let { total -> if (total > 0) (c.value / total) * 100f else 0f }
            val text = "${c.label}: ${String.format(Locale.getDefault(),"%.1f", pct)}%"
            drawEllipsizedText(canvas, text, legendXStart + 12f, legendY, bodyPaint, (PAGE_WIDTH - MARGIN - legendXStart) - 10f)
            legendY += 16f
        }
        y += pieSize + 20f

        // Compact Top Apps table — Rank, App, Time (top 8, >=1m)
        canvas.drawText("Top Applications", MARGIN.toFloat(), y, headerPaint)
        y += 16f
        val topRows = appAnalytics
            .sortedByDescending { it.totalTimeMs }
            .filter { it.totalTimeMs >= MIN_APP_ROW_MS }
            .take(8)
        if (topRows.isNotEmpty()) {
            val cols = listOf(
                TableCol(CONTENT_WIDTH * 0.12f),  // Rank
                TableCol(CONTENT_WIDTH * 0.60f),  // App
                TableCol(CONTENT_WIDTH * 0.28f)   // Time
            )
            // Header row
            var x = MARGIN.toFloat()
            val headerY = y
            val bg = Paint().apply { color = Color.rgb(246,246,246) }
            canvas.drawRect(MARGIN.toFloat(), headerY - 12f, (PAGE_WIDTH - MARGIN).toFloat(), headerY + 8f, bg)
            listOf("#", "App", "Time").forEachIndexed { i, label ->
                canvas.drawText(label, x + 4f, headerY, headerPaint)
                x += cols[i].width
            }
            y += 18f
            // Rows
            val zebra = Paint().apply { color = Color.rgb(252,252,252) }
            topRows.forEachIndexed { i, row ->
                ensureSpace(18f, title, subtitle)
                if (i % 2 == 1) canvas.drawRect(MARGIN.toFloat(), y - 12f, (PAGE_WIDTH - MARGIN).toFloat(), y + 8f, zebra)
                var cx = MARGIN.toFloat()
                drawCellText(canvas, (i + 1).toString(), cx + 4f, y, bodyPaint); cx += cols[0].width
                val safeName = sanitizeAppName(row.appName, row.packageName).replace("%1\$s", "").trim().ifBlank { row.packageName ?: "Unknown" }
                drawCellText(canvas, ellipsize(safeName, bodyPaint, cols[1].width - 8f), cx + 4f, y, bodyPaint); cx += cols[1].width
                drawCellText(canvas, formatDuration(row.totalTimeMs), cx + 4f, y, bodyPaint)
                y += 18f
            }
        } else {
            canvas.drawText("Not enough usage to highlight apps today", MARGIN.toFloat(), y, faintPaint)
            y += 16f
        }

        finishPageIfNeeded()

        val fileName = "ScrollSense_Daily_${dailySummary.date}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        file
    }

    /**
     * Export weekly (or monthly) report as PDF
     */
    suspend fun exportWeeklyReport(weeklyAnalytics: WeeklyAnalytics): File = withContext(Dispatchers.IO) {

        val document = PdfDocument()

        // Page utilities
        var pageIndex = 0
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = 0f

        fun footer() {
            val footerText = "ScrollSense • ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())} • Page $pageIndex"
            val yFooter = PAGE_HEIGHT - (FOOTER_HEIGHT / 2f)
            canvas.drawLine(MARGIN.toFloat(), yFooter - 10f, (PAGE_WIDTH - MARGIN).toFloat(), yFooter - 10f, rulePaint)
            canvas.drawText(footerText, MARGIN.toFloat(), yFooter, faintPaint)
        }

        fun startPage(title: String, subtitle: String) {
            pageIndex += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
            page = document.startPage(info)
            canvas = page.canvas
            var headerY = MARGIN + 6f
            canvas.drawText(title, MARGIN.toFloat(), headerY + titlePaint.textSize, titlePaint)
            headerY += titlePaint.textSize + 6f
            canvas.drawText(subtitle, MARGIN.toFloat(), headerY + subHeaderPaint.textSize, subHeaderPaint)
            headerY += subHeaderPaint.textSize + 10f
            canvas.drawLine(MARGIN.toFloat(), headerY, (PAGE_WIDTH - MARGIN).toFloat(), headerY, rulePaint)
            y = headerY + 18f
        }

        fun finishPageIfNeeded() { footer(); document.finishPage(page) }

        fun ensureSpace(required: Float, title: String, subtitle: String) {
            val bottomLimit = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT
            if (y + required > bottomLimit) { finishPageIfNeeded(); startPage(title, subtitle) }
        }

        val title = "ScrollSense Weekly Report"
        val subtitle = "Period: ${weeklyAnalytics.startDate} to ${weeklyAnalytics.endDate}"
        startPage(title, subtitle)

        // Weekly summary + highlights
        canvas.drawText("Weekly Summary", MARGIN.toFloat(), y, headerPaint)
        y += 20f
        val weeklyStats = listOf(
            "Total Screen Time: ${formatDuration(weeklyAnalytics.totalScreenTimeMs)}",
            "Daily Average: ${formatDuration(weeklyAnalytics.totalScreenTimeMs / 7)}",
            "Total Sessions: ${weeklyAnalytics.totalSessions}",
            "Average Session: ${formatDuration(weeklyAnalytics.averageSessionDurationMs)}"
        )
        weeklyStats.forEach { stat -> ensureSpace(16f, title, subtitle); canvas.drawText(stat, MARGIN.toFloat(), y, bodyPaint); y += 16f }

        // Highlights line: top app and busiest day
        val busiest = weeklyAnalytics.dailySummaries.maxByOrNull { it.totalScreenTimeMs }
        val calmest = weeklyAnalytics.dailySummaries.minByOrNull { it.totalScreenTimeMs }
        val topApp = weeklyAnalytics.appData.maxByOrNull { it.totalTimeMs }
        val highlight = buildString {
            if (busiest != null) append("Busiest day: ${busiest.date} • ${formatDuration(busiest.totalScreenTimeMs)}    ")
            if (calmest != null) append("Quietest: ${calmest.date} • ${formatDuration(calmest.totalScreenTimeMs)}    ")
            if (topApp != null) append("Top app: ${sanitizeAppName(topApp.appName, topApp.packageName)} • ${formatDuration(topApp.totalTimeMs)}")
        }
        if (highlight.isNotBlank()) { canvas.drawText(highlight, MARGIN.toFloat(), y, faintPaint); y += 16f }

        y += 6f
        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, rulePaint)
        y += 14f

        // Trend chart
        canvas.drawText("Daily Screen Time Trend", MARGIN.toFloat(), y, headerPaint)
        y += 18f
        val chartHeight = 160f
        val bottomLabelSpace = if (weeklyAnalytics.dailySummaries.size > 10) 36f else 24f
        ensureSpace(chartHeight + bottomLabelSpace, title, subtitle)
        drawBarChart(canvas, y, weeklyAnalytics.dailySummaries)
        y += chartHeight + bottomLabelSpace

        // Category analysis (Top 6)
        canvas.drawText("Category Analysis", MARGIN.toFloat(), y, headerPaint)
        y += 18f
        weeklyAnalytics.categoryData
            .sortedByDescending { it.totalTimeMs }
            .take(6)
            .forEachIndexed { index, category ->
                ensureSpace(16f, title, subtitle)
                val percentage = if (weeklyAnalytics.totalScreenTimeMs > 0) (category.totalTimeMs.toFloat() / weeklyAnalytics.totalScreenTimeMs) * 100f else 0f
                chartPaint.color = getChartColor(index)
                canvas.drawCircle(MARGIN + 5f, y - 5f, 5f, chartPaint)
                val text = "${category.category}: ${formatDuration(category.totalTimeMs)} (${String.format(Locale.getDefault(),"%.1f", percentage)}%)"
                canvas.drawText(text, (MARGIN + 18).toFloat(), y, bodyPaint)
                y += 16f
            }

        y += 6f
        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, rulePaint)
        y += 14f

        // Weekly Top Apps (Top 10, >=1m)
        canvas.drawText("Top Applications", MARGIN.toFloat(), y, headerPaint)
        y += 16f
        val topApps = weeklyAnalytics.appData
            .sortedByDescending { it.totalTimeMs }
            .filter { it.totalTimeMs >= MIN_APP_ROW_MS }
            .take(10)
        if (topApps.isNotEmpty()) {
            val cols = listOf(
                TableCol(CONTENT_WIDTH * 0.12f),
                TableCol(CONTENT_WIDTH * 0.60f),
                TableCol(CONTENT_WIDTH * 0.28f)
            )
            var x = MARGIN.toFloat()
            val headerY = y
            val bg = Paint().apply { color = Color.rgb(246,246,246) }
            canvas.drawRect(MARGIN.toFloat(), headerY - 12f, (PAGE_WIDTH - MARGIN).toFloat(), headerY + 8f, bg)
            listOf("#", "App", "Time").forEachIndexed { i, label ->
                canvas.drawText(label, x + 4f, headerY, headerPaint)
                x += cols[i].width
            }
            y += 18f
            val zebra = Paint().apply { color = Color.rgb(252,252,252) }
            topApps.forEachIndexed { i, app ->
                ensureSpace(18f, title, subtitle)
                if (i % 2 == 1) canvas.drawRect(MARGIN.toFloat(), y - 12f, (PAGE_WIDTH - MARGIN).toFloat(), y + 8f, zebra)
                var cx = MARGIN.toFloat()
                drawCellText(canvas, (i + 1).toString(), cx + 4f, y, bodyPaint); cx += cols[0].width
                val safe = sanitizeAppName(app.appName, app.packageName).replace("%1\$s", "").trim().ifBlank { app.packageName ?: "Unknown" }
                drawCellText(canvas, ellipsize(safe, bodyPaint, cols[1].width - 8f), cx + 4f, y, bodyPaint); cx += cols[1].width
                drawCellText(canvas, formatDuration(app.totalTimeMs), cx + 4f, y, bodyPaint)
                y += 18f
            }
        } else {
            canvas.drawText("No notable app usage this period", MARGIN.toFloat(), y, faintPaint); y += 16f
        }

        finishPageIfNeeded()

        val fileName = "ScrollSense_Weekly_${weeklyAnalytics.startDate}_to_${weeklyAnalytics.endDate}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        file
    }

    // ---------- Drawing helpers ----------

    private fun drawPieChart(canvas: Canvas, rect: RectF, data: List<ChartData>) {
        val clean = data.filter { it.value > 0f }
        if (clean.isEmpty()) return
        val total = clean.sumOf { it.value.toDouble() }.toFloat()
        var startAngle = 0f
        clean.forEachIndexed { index, item ->
            val sweepAngle = (item.value / total) * 360f
            chartPaint.color = getChartColor(index)
            canvas.drawArc(rect, startAngle, sweepAngle, true, chartPaint)
            startAngle += sweepAngle
        }
    }

    private fun drawBarChart(canvas: Canvas, startY: Float, dailySummaries: List<DailySummary>) {
        // Layout constants
        val chartHeight = 160f
        val n = dailySummaries.size.coerceAtLeast(1)
        // Use smaller gaps when many bars, keep left+right padding by having N+1 gaps
        val gap = if (n > 20) 3f else 6f
        val totalGaps = n + 1
        var barWidth = (CONTENT_WIDTH.toFloat() - gap * totalGaps) / n
        if (barWidth < 6f) barWidth = 6f
        val startX = MARGIN.toFloat() + gap

        val maxValue = (dailySummaries.maxOfOrNull { it.totalScreenTimeMs } ?: 1L).coerceAtLeast(1L)

        // Label paint centered for accurate alignment
        val labelPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER }
        val valuePaint = Paint(faintPaint).apply { textAlign = Paint.Align.CENTER; textSize = 11f }
        val rotateLabels = n > 10
        val labelStep = kotlin.math.ceil(n / 10f).toInt().coerceAtLeast(1)
        val bottom = startY + chartHeight

        dailySummaries.forEachIndexed { index, summary ->
            val barHeight = (summary.totalScreenTimeMs.toFloat() / maxValue) * chartHeight
            val left = startX + (index * (barWidth + gap))
            val top = bottom - barHeight
            val right = left + barWidth
            chartPaint.color = getChartColor(index % 10)
            canvas.drawRect(left, top, right, bottom, chartPaint)

            // Duration value label
            val valueLabel = formatDuration(summary.totalScreenTimeMs)
            val cx = left + barWidth / 2f
            val valueY = (top - 4f).coerceAtLeast(startY + 10f)
            canvas.drawText(valueLabel, cx, valueY, valuePaint)

            // Compute label text: 3-letter day for small N; day-of-month for larger ranges
            val dayLabel = try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(summary.date) ?: Date()
                if (n <= 10) SimpleDateFormat("E", Locale.getDefault()).format(date)
                else SimpleDateFormat("d", Locale.getDefault()).format(date)
            } catch (_: Exception) { summary.date }

            val baseY = bottom + if (rotateLabels) 18f else 14f
            if (index % labelStep == 0) {
                if (rotateLabels) {
                    canvas.save()
                    canvas.rotate(-45f, cx, baseY)
                    canvas.drawText(dayLabel, cx, baseY, labelPaint)
                    canvas.restore()
                } else {
                    canvas.drawText(dayLabel, cx, baseY, labelPaint)
                }
            }
        }
    }

    private data class TableCol(val width: Float)

    private fun tableColumns(): List<TableCol> {
        // Content width split
        val nameW = CONTENT_WIDTH * 0.46f
        val catW = CONTENT_WIDTH * 0.20f
        val timeW = CONTENT_WIDTH * 0.16f
        val sessW = CONTENT_WIDTH * 0.18f
        return listOf(TableCol(nameW), TableCol(catW), TableCol(timeW), TableCol(sessW))
    }

    private fun drawTableHeaderCells(canvas: Canvas, cols: List<TableCol>, baseY: Float, paint: Paint) {
        var x = MARGIN.toFloat()
        val labels = listOf("App Name", "Category", "Time", "Sessions")
        labels.forEachIndexed { i, label ->
            canvas.drawText(label, x + 4f, baseY, paint)
            x += cols[i].width
        }
    }

    private fun drawCellText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        canvas.drawText(text, x, y, paint)
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var low = 0
        var high = text.length
        var best = ""
        while (low <= high) {
            val mid = (low + high) / 2
            val candidate = if (mid <= 2) "…" else text.substring(0, mid - 1) + "…"
            val w = paint.measureText(candidate)
            if (w <= maxWidth) { best = candidate; low = mid + 1 } else { high = mid - 1 }
        }
        return best
    }

    private fun drawEllipsizedText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, maxWidth: Float) {
        canvas.drawText(ellipsize(text, paint, maxWidth), x, y, paint)
    }

    private fun getChartColor(index: Int): Int {
        val colors = arrayOf(
            Color.parseColor("#FF6B6B"), // Red
            Color.parseColor("#4ECDC4"), // Teal
            Color.parseColor("#45B7D1"), // Blue
            Color.parseColor("#96CEB4"), // Green
            Color.parseColor("#FFEAA7"), // Yellow
            Color.parseColor("#DDA0DD"), // Plum
            Color.parseColor("#98D8C8"), // Mint
            Color.parseColor("#F7DC6F"), // Light Yellow
            Color.parseColor("#BB8FCE"), // Light Purple
            Color.parseColor("#85C1E9")  // Light Blue
        )
        return colors[index % colors.size]
    }

    private fun formatDuration(durationMs: Long): String {
        val totalMinutes = durationMs / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    // Normalize known package/app labels for readability in PDFs
    private fun sanitizeAppName(appName: String, packageName: String?): String {
        val raw = appName.ifBlank { packageName ?: "" }
        val key = ((packageName ?: "") + "|" + raw).lowercase(Locale.getDefault())
        // guard against format-string artifacts like %1$s
        val cleanRaw = raw.replace(Regex("%\\d+\\\$s"), "").trim()
        return when {
            key.contains("incallui") || key.contains("dialer") || key.contains("telecom") -> "call"
            key.contains("com.android.chrome") || key.contains("org.chromium.chrome") || key.contains(" chrome") || key.endsWith(".chrome") -> "chrome"
            cleanRaw.isBlank() -> packageName?.substringAfterLast('.')?.replaceFirstChar { it.uppercase() } ?: raw
            else -> cleanRaw
        }
    }
}

data class ChartData(val label: String, val value: Float)
