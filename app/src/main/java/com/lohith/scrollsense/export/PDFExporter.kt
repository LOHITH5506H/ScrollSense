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

class PDFExporter(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 40
        private const val CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN)
        private const val FOOTER_HEIGHT = 24
        private const val MIN_APP_ROW_MS = 60_000L
    }

    private val titlePaint = Paint().apply {
        textSize = 26f; color = Color.BLACK; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
    }
    private val headerPaint = Paint().apply {
        textSize = 16f; color = Color.BLACK; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
    }
    private val subHeaderPaint = Paint().apply {
        textSize = 12.5f; color = Color.DKGRAY; isAntiAlias = true
    }
    private val bodyPaint = Paint().apply {
        textSize = 12f; color = Color.BLACK; isAntiAlias = true
    }
    private val italicBodyPaint = Paint().apply {
        textSize = 12f; color = Color.BLACK; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }
    private val faintPaint = Paint().apply {
        textSize = 11f; color = Color.rgb(150, 150, 150); isAntiAlias = true
    }
    private val rulePaint = Paint().apply {
        color = Color.rgb(220, 220, 220); strokeWidth = 1.2f; isAntiAlias = true
    }
    private val chartPaint = Paint().apply {
        isAntiAlias = true; style = Paint.Style.FILL
    }

    suspend fun exportDailyReport(
        dailySummary: DailySummary,
        categoryAnalytics: List<com.lohith.scrollsense.data.DailyCategoryAnalytics>,
        appAnalytics: List<com.lohith.scrollsense.data.DailyAppAnalytics>,
        insightText: String
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
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
            pageIndex++; val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create(); page = document.startPage(info); canvas = page.canvas
            var headerY = MARGIN + 6f
            canvas.drawText(title, MARGIN.toFloat(), headerY + titlePaint.textSize, titlePaint); headerY += titlePaint.textSize + 6f
            canvas.drawText(subtitle, MARGIN.toFloat(), headerY + subHeaderPaint.textSize, subHeaderPaint); headerY += subHeaderPaint.textSize + 10f
            canvas.drawLine(MARGIN.toFloat(), headerY, (PAGE_WIDTH - MARGIN).toFloat(), headerY, rulePaint); y = headerY + 24f
        }

        fun finishPageIfNeeded() { footer(); document.finishPage(page) }

        fun ensureSpace(required: Float, title: String, subtitle: String) {
            if (y + required > PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT) { finishPageIfNeeded(); startPage(title, subtitle) }
        }

        val title = "ScrollSense Daily Report"
        val subtitle = "Date: ${dailySummary.date}"
        startPage(title, subtitle)

        canvas.drawText("Daily Summary", MARGIN.toFloat(), y, headerPaint); y += 22f
        val k1 = "Total Screen Time: ${formatDuration(dailySummary.totalScreenTimeMs)}"
        val k2 = "Number of Sessions: ${dailySummary.sessionsCount}"
        val k3 = "Average Session: ${formatDuration(dailySummary.averageSessionDurationMs)}"
        val topAppClean = sanitizeAppName(dailySummary.topApp ?: "-", null).replaceFirstChar { it.uppercase() }
        val k4 = "Most Used App: ${if (topAppClean.isBlank()) "-" else topAppClean}"
        listOf(k1, k2, k3, k4).chunked(2).forEach { pair ->
            ensureSpace(18f, title, subtitle); canvas.drawText(pair[0], MARGIN.toFloat(), y, bodyPaint)
            if (pair.size > 1) canvas.drawText(pair[1], (MARGIN + CONTENT_WIDTH / 2f), y, bodyPaint)
            y += 18f
        }

        y += 24f; canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, rulePaint); y += 24f

        canvas.drawText("Category Breakdown", MARGIN.toFloat(), y, headerPaint); y += 18f
        val pieSize = 170f
        ensureSpace(pieSize + 18f, title, subtitle)
        val catSorted = categoryAnalytics.sortedByDescending { it.totalTimeMs }
        val (topCats, others) = if (catSorted.size > 5) catSorted.take(5) to catSorted.drop(5) else catSorted to emptyList()
        val othersMs = others.sumOf { it.totalTimeMs }
        val pieData = buildList {
            topCats.forEach { add(ChartData(it.category, it.totalTimeMs.toFloat())) }
            if (othersMs > 0) add(ChartData("Others", othersMs.toFloat()))
        }
        drawPieChart(canvas, RectF(MARGIN.toFloat(), y, MARGIN + pieSize, y + pieSize), pieData)
        val legendXStart = MARGIN + pieSize + 14f; var legendY = y + 8f
        pieData.forEachIndexed { idx, c ->
            chartPaint.color = getChartColor(idx); canvas.drawCircle(legendXStart, legendY - 4f, 5f, chartPaint)
            val pct = pieData.sumOf { it.value.toDouble() }.toFloat().let { total -> if (total > 0) (c.value / total) * 100f else 0f }
            val text = "${c.label}: ${String.format(Locale.getDefault(), "%.1f%%", pct)}"
            drawEllipsizedText(canvas, text, legendXStart + 12f, legendY, bodyPaint, (PAGE_WIDTH - MARGIN - legendXStart) - 10f); legendY += 16f
        }
        y += pieSize + 24f

        val topRows = appAnalytics.sortedByDescending { it.totalTimeMs }.filter { it.totalTimeMs >= MIN_APP_ROW_MS }.take(8)
        if (topRows.isNotEmpty()) {
            val sectionHeaderHeight = 36f
            val tableHeaderHeight = 18f
            val tableRowsHeight = topRows.size * 18f
            val totalRequiredHeight = sectionHeaderHeight + tableHeaderHeight + tableRowsHeight
            ensureSpace(totalRequiredHeight, title, subtitle)

            canvas.drawText("Top Applications", MARGIN.toFloat(), y, headerPaint); y += 18f
            val cols = listOf(TableCol(CONTENT_WIDTH * 0.12f), TableCol(CONTENT_WIDTH * 0.60f), TableCol(CONTENT_WIDTH * 0.28f))
            var x = MARGIN.toFloat(); val headerY = y; val bg = Paint().apply { color = Color.rgb(246, 246, 246) }
            canvas.drawRect(MARGIN.toFloat(), headerY - 12f, (PAGE_WIDTH - MARGIN).toFloat(), headerY + 8f, bg)
            listOf("#", "App", "Time").forEachIndexed { i, label -> canvas.drawText(label, x + 4f, headerY, headerPaint); x += cols[i].width }; y += 18f
            val zebra = Paint().apply { color = Color.rgb(252, 252, 252) }
            topRows.forEachIndexed { i, row ->
                if (i % 2 == 1) canvas.drawRect(MARGIN.toFloat(), y - 12f, (PAGE_WIDTH - MARGIN).toFloat(), y + 8f, zebra)
                var cx = MARGIN.toFloat(); drawCellText(canvas, (i + 1).toString(), cx + 4f, y, bodyPaint); cx += cols[0].width
                val safeName = sanitizeAppName(row.appName, row.packageName); drawCellText(canvas, ellipsize(safeName, bodyPaint, cols[1].width - 8f), cx + 4f, y, bodyPaint); cx += cols[1].width
                drawCellText(canvas, formatDuration(row.totalTimeMs), cx + 4f, y, bodyPaint); y += 18f
            }
        } else {
            canvas.drawText("Not enough usage to highlight apps today", MARGIN.toFloat(), y, faintPaint); y += 16f
        }

        if (insightText.isNotBlank()) {
            val insightLines = insightText.split('\n').filter { it.isNotBlank() }.flatMap { wrapText(it, italicBodyPaint, CONTENT_WIDTH.toFloat()) }
            val requiredInsightHeight = 48f + (insightLines.size * 18f)
            ensureSpace(requiredInsightHeight, title, subtitle)
            y += 24f; canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, rulePaint); y += 24f
            canvas.drawText("AI-Powered Insight", MARGIN.toFloat(), y, headerPaint); y += 20f
            insightLines.forEach { line ->
                canvas.drawText(line, MARGIN.toFloat(), y, italicBodyPaint); y += 18f
            }
        }

        finishPageIfNeeded()
        val fileName = "ScrollSense_Daily_${dailySummary.date}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        FileOutputStream(file).use { document.writeTo(it) }; document.close(); return@withContext file
    }

    suspend fun exportWeeklyReport(
        weeklyAnalytics: WeeklyAnalytics,
        insightText: String,
        isMonthlyReport: Boolean
    ): File = withContext(Dispatchers.IO) {

        val document = PdfDocument(); var pageIndex = 0; lateinit var page: PdfDocument.Page; lateinit var canvas: Canvas; var y = 0f
        fun footer() {
            val footerText = "ScrollSense • ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())} • Page $pageIndex"
            val yFooter = PAGE_HEIGHT - (FOOTER_HEIGHT / 2f)
            canvas.drawLine(MARGIN.toFloat(), yFooter - 10f, (PAGE_WIDTH - MARGIN).toFloat(), yFooter - 10f, rulePaint)
            canvas.drawText(footerText, MARGIN.toFloat(), yFooter, faintPaint)
        }
        fun startPage(title: String, subtitle: String) {
            pageIndex++; val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create(); page = document.startPage(info); canvas = page.canvas
            var headerY = MARGIN + 6f
            canvas.drawText(title, MARGIN.toFloat(), headerY + titlePaint.textSize, titlePaint); headerY += titlePaint.textSize + 6f
            canvas.drawText(subtitle, MARGIN.toFloat(), headerY + subHeaderPaint.textSize, subHeaderPaint); headerY += subHeaderPaint.textSize + 10f
            canvas.drawLine(MARGIN.toFloat(), headerY, (PAGE_WIDTH - MARGIN).toFloat(), headerY, rulePaint); y = headerY + 24f
        }
        fun finishPageIfNeeded() { footer(); document.finishPage(page) }
        fun ensureSpace(required: Float, title: String, subtitle: String) {
            if (y + required > PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT) { finishPageIfNeeded(); startPage(title, subtitle) }
        }

        val reportType = if (isMonthlyReport) "Monthly" else "Weekly"
        val title = "ScrollSense $reportType Report"
        val subtitle = "Period: ${weeklyAnalytics.startDate} to ${weeklyAnalytics.endDate}"
        startPage(title, subtitle)

        canvas.drawText("$reportType Summary", MARGIN.toFloat(), y, headerPaint); y += 20f
        val dailyAvg = if (weeklyAnalytics.dailySummaries.isNotEmpty()) weeklyAnalytics.totalScreenTimeMs / weeklyAnalytics.dailySummaries.size else 0L
        val weeklyStats = listOf(
            "Total Screen Time: ${formatDuration(weeklyAnalytics.totalScreenTimeMs)}", "Daily Average: ${formatDuration(dailyAvg)}",
            "Total Sessions: ${weeklyAnalytics.totalSessions}", "Average Session: ${formatDuration(weeklyAnalytics.averageSessionDurationMs)}"
        )
        weeklyStats.forEach { stat -> ensureSpace(18f, title, subtitle); canvas.drawText(stat, MARGIN.toFloat(), y, bodyPaint); y += 18f }

        y += 24f; canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, rulePaint); y += 24f

        val chartTitle = if (isMonthlyReport) "Weekly Screen Time Trend" else "Daily Screen Time Trend"
        // --- FIX: Increased spacing between title and chart ---
        canvas.drawText(chartTitle, MARGIN.toFloat(), y, headerPaint); y += 30f
        val chartHeight = 160f; val bottomLabelSpace = 36f
        ensureSpace(chartHeight + bottomLabelSpace, title, subtitle)
        drawBarChart(canvas, y, weeklyAnalytics.dailySummaries)
        y += chartHeight + bottomLabelSpace

        y += 24f; canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, rulePaint); y += 24f

        canvas.drawText("Category Breakdown", MARGIN.toFloat(), y, headerPaint); y += 18f
        val pieSize = 170f
        ensureSpace(pieSize + 18f, title, subtitle)
        val catSorted = weeklyAnalytics.categoryData.sortedByDescending { it.totalTimeMs }
        val (topCats, others) = if (catSorted.size > 5) catSorted.take(5) to catSorted.drop(5) else catSorted to emptyList()
        val othersMs = others.sumOf { it.totalTimeMs }
        val pieData = buildList {
            topCats.forEach { add(ChartData(it.category, it.totalTimeMs.toFloat())) }
            if (othersMs > 0) add(ChartData("Others", othersMs.toFloat()))
        }
        drawPieChart(canvas, RectF(MARGIN.toFloat(), y, MARGIN + pieSize, y + pieSize), pieData)
        val legendXStart = MARGIN + pieSize + 14f; var legendY = y + 8f
        pieData.forEachIndexed { idx, c ->
            chartPaint.color = getChartColor(idx); canvas.drawCircle(legendXStart, legendY - 4f, 5f, chartPaint)
            val pct = pieData.sumOf { it.value.toDouble() }.toFloat().let { total -> if (total > 0) (c.value / total) * 100f else 0f }
            val text = "${c.label}: ${String.format(Locale.getDefault(), "%.1f%%", pct)}"
            drawEllipsizedText(canvas, text, legendXStart + 12f, legendY, bodyPaint, (PAGE_WIDTH - MARGIN - legendXStart) - 10f); legendY += 16f
        }
        y += pieSize + 24f

        val topApps = weeklyAnalytics.appData.sortedByDescending { it.totalTimeMs }.filter { it.totalTimeMs >= MIN_APP_ROW_MS }.take(10)
        if (topApps.isNotEmpty()) {
            val sectionHeaderHeight = 36f
            val tableHeaderHeight = 18f
            val tableRowsHeight = topApps.size * 18f
            val totalRequiredHeight = sectionHeaderHeight + tableHeaderHeight + tableRowsHeight
            ensureSpace(totalRequiredHeight, title, subtitle)

            canvas.drawText("Top Applications", MARGIN.toFloat(), y, headerPaint); y += 18f
            val cols = listOf(TableCol(CONTENT_WIDTH * 0.12f), TableCol(CONTENT_WIDTH * 0.60f), TableCol(CONTENT_WIDTH * 0.28f))
            var x = MARGIN.toFloat(); val headerY = y; val bg = Paint().apply { color = Color.rgb(246, 246, 246) }
            canvas.drawRect(MARGIN.toFloat(), headerY - 12f, (PAGE_WIDTH - MARGIN).toFloat(), headerY + 8f, bg)
            listOf("#", "App", "Time").forEachIndexed { i, label -> canvas.drawText(label, x + 4f, headerY, headerPaint); x += cols[i].width }; y += 18f
            val zebra = Paint().apply { color = Color.rgb(252, 252, 252) }
            topApps.forEachIndexed { i, app ->
                if (i % 2 == 1) canvas.drawRect(MARGIN.toFloat(), y - 12f, (PAGE_WIDTH - MARGIN).toFloat(), y + 8f, zebra)
                var cx = MARGIN.toFloat(); drawCellText(canvas, (i + 1).toString(), cx + 4f, y, bodyPaint); cx += cols[0].width
                val safe = sanitizeAppName(app.appName, app.packageName); drawCellText(canvas, ellipsize(safe, bodyPaint, cols[1].width - 8f), cx + 4f, y, bodyPaint); cx += cols[1].width
                drawCellText(canvas, formatDuration(app.totalTimeMs), cx + 4f, y, bodyPaint); y += 18f
            }
        } else {
            canvas.drawText("No notable app usage this period", MARGIN.toFloat(), y, faintPaint); y += 16f
        }

        if (insightText.isNotBlank()) {
            val insightLines = insightText.split('\n').filter { it.isNotBlank() }.flatMap { wrapText(it, italicBodyPaint, CONTENT_WIDTH.toFloat()) }
            val requiredInsightHeight = 48f + (insightLines.size * 18f)
            ensureSpace(requiredInsightHeight, title, subtitle)
            y += 24f; canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, rulePaint); y += 24f
            canvas.drawText("AI-Powered Insight", MARGIN.toFloat(), y, headerPaint); y += 20f
            insightLines.forEach { line ->
                canvas.drawText(line, MARGIN.toFloat(), y, italicBodyPaint); y += 18f
            }
        }

        finishPageIfNeeded()
        val fileName = "ScrollSense_${reportType}_${weeklyAnalytics.startDate}_to_${weeklyAnalytics.endDate}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        FileOutputStream(file).use { document.writeTo(it) }; document.close(); return@withContext file
    }

    private fun drawPieChart(canvas: Canvas, rect: RectF, data: List<ChartData>) {
        val clean = data.filter { it.value > 0f }; if (clean.isEmpty()) return
        val total = clean.sumOf { it.value.toDouble() }.toFloat(); var startAngle = 0f
        clean.forEachIndexed { index, item ->
            val sweepAngle = (item.value / total) * 360f; chartPaint.color = getChartColor(index)
            canvas.drawArc(rect, startAngle, sweepAngle, true, chartPaint); startAngle += sweepAngle
        }
    }

    private fun drawBarChart(canvas: Canvas, startY: Float, dailySummaries: List<DailySummary>) {
        val sortedSummaries = dailySummaries.sortedBy { it.date }
        val chartHeight = 160f; val bottom = startY + chartHeight
        val labelPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER }
        // --- FIX: Darker color for value labels ---
        val valuePaint = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER; textSize = 10f; color = Color.DKGRAY }

        if (sortedSummaries.size > 10) { // Monthly view: group by week
            val weeklyChunks = sortedSummaries.chunked(7)
            val weeklyTotals = weeklyChunks.map { week -> week.sumOf { it.totalScreenTimeMs } }
            val maxValue = (weeklyTotals.maxOrNull() ?: 1L).coerceAtLeast(1L)
            val n = weeklyTotals.size; val gap = 10f
            val barWidth = (CONTENT_WIDTH.toFloat() - gap * (n + 1)) / n
            val startX = MARGIN.toFloat() + gap

            weeklyTotals.forEachIndexed { index, totalMs ->
                val barHeight = (totalMs.toFloat() / maxValue) * chartHeight
                val left = startX + (index * (barWidth + gap)); val top = bottom - barHeight; val right = left + barWidth
                chartPaint.color = getChartColor(index); canvas.drawRect(left, top, right, bottom, chartPaint)
                val valueLabel = formatDuration(totalMs); val cx = left + barWidth / 2f
                canvas.drawText(valueLabel, cx, (top - 5f).coerceAtLeast(startY + 12f), valuePaint)
                canvas.drawText("Week ${index + 1}", cx, bottom + 14f, labelPaint)
            }
        } else { // Daily / Weekly view
            val n = sortedSummaries.size.coerceAtLeast(1); val gap = if (n > 20) 3f else 6f
            val barWidth = (CONTENT_WIDTH.toFloat() - gap * (n + 1)) / n; val startX = MARGIN.toFloat() + gap
            val maxValue = (sortedSummaries.maxOfOrNull { it.totalScreenTimeMs } ?: 1L).coerceAtLeast(1L)

            sortedSummaries.forEachIndexed { index, summary ->
                val barHeight = (summary.totalScreenTimeMs.toFloat() / maxValue) * chartHeight
                val left = startX + (index * (barWidth + gap)); val top = bottom - barHeight; val right = left + barWidth
                chartPaint.color = getChartColor(index % 10); canvas.drawRect(left, top, right, bottom, chartPaint)
                val valueLabel = formatDuration(summary.totalScreenTimeMs); val cx = left + barWidth / 2f
                canvas.drawText(valueLabel, cx, (top - 5f).coerceAtLeast(startY + 12f), valuePaint)
                val dayLabel = try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(summary.date) ?: Date()
                    SimpleDateFormat("E", Locale.getDefault()).format(date)
                } catch (_: Exception) { summary.date }
                canvas.drawText(dayLabel, cx, bottom + 14f, labelPaint)
            }
        }
    }

    private fun drawCellText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        canvas.drawText(text, x, y, paint)
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var low = 0; var high = text.length; var best = ""
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

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(' ')
        var currentLine = StringBuilder()
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                if (currentLine.isNotEmpty()) currentLine.append(' ')
                currentLine.append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun getChartColor(index: Int): Int {
        val colors = arrayOf(
            Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"), Color.parseColor("#45B7D1"),
            Color.parseColor("#96CEB4"), Color.parseColor("#FFEAA7"), Color.parseColor("#DDA0DD"),
            Color.parseColor("#98D8C8"), Color.parseColor("#F7DC6F"), Color.parseColor("#BB8FCE"),
            Color.parseColor("#85C1E9")
        )
        return colors[index % colors.size]
    }

    private fun formatDuration(durationMs: Long): String {
        val totalMinutes = durationMs / (1000 * 60); val hours = totalMinutes / 60; val minutes = totalMinutes % 60
        return when { hours > 0 -> "${hours}h ${minutes}m"; minutes > 0 -> "${minutes}m"; else -> "<1m" }
    }

    private fun sanitizeAppName(appName: String, packageName: String?): String {
        val raw = appName.ifBlank { packageName ?: "" }; val key = ((packageName ?: "") + "|" + raw).lowercase(Locale.getDefault())
        val cleanRaw = raw.replace(Regex("%\\d+\\\$s"), "").trim()
        return when {
            key.contains("incallui") || key.contains("dialer") || key.contains("telecom") -> "Call"
            key.contains("com.android.chrome") || key.contains("org.chromium.chrome") -> "Chrome"
            cleanRaw.isBlank() -> packageName?.substringAfterLast('.')?.replaceFirstChar { it.uppercase() } ?: raw
            else -> cleanRaw
        }
    }
}

private data class ChartData(val label: String, val value: Float)
private data class TableCol(val width: Float)