package com.lohith.scrollsense.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.lohith.scrollsense.analytics.AnalyticsProcessor
import com.lohith.scrollsense.databinding.ActivityEnhancedAnalyticsBinding
import com.lohith.scrollsense.export.PDFExporter
import com.lohith.scrollsense.viewmodel.EnhancedAnalyticsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EnhancedAnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnhancedAnalyticsBinding
    private val viewModel: EnhancedAnalyticsViewModel by viewModels()
    private lateinit var analyticsProcessor: AnalyticsProcessor
    private lateinit var pdfExporter: PDFExporter

    // Keep latest datasets to compute Detailed Statistics cohesively
    private var lastSummaries: List<com.lohith.scrollsense.data.DailySummary> = emptyList()
    private var lastCategories: List<com.lohith.scrollsense.analytics.WeeklyCategoryData> = emptyList()
    private var lastApps: List<com.lohith.scrollsense.analytics.WeeklyAppData> = emptyList()

    // Use a lambda-enabled adapter so tapping a row does something useful
    private lateinit var detailedAdapter: DetailedStatsAdapter

    // Programmatic legend group injected below the pie chart
    private var categoryLegendGroup: ChipGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnhancedAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        analyticsProcessor = AnalyticsProcessor(this)
        pdfExporter = PDFExporter(this)

        // Keep pie chart big regardless of legend lines
        binding.pieChartCategories.post {
            val lp = binding.pieChartCategories.layoutParams
            val minHeight = dpToPxInt(240f)
            if (lp.height < minHeight) {
                lp.height = minHeight
                binding.pieChartCategories.layoutParams = lp
            }
        }

        setupViews()
        setupObservers()

        // Process more history so Month view has distinct data
        lifecycleScope.launch {
            analyticsProcessor.processLastDays(30)
            viewModel.refreshData()
        }
    }

    // Generate N visually distinct colors (HSV evenly spaced hues)
    private fun generateDistinctColors(count: Int, saturation: Float = 0.70f, value: Float = 0.95f): List<Int> {
        if (count <= 0) return emptyList()
        val colors = ArrayList<Int>(count)
        val step = 360f / count
        for (i in 0 until count) {
            val alt = if (i % 2 == 0) 0.95f else value
            val hsv = floatArrayOf((i * step) % 360f, saturation, alt)
            colors.add(Color.HSVToColor(hsv))
        }
        return colors
    }

    private fun setupViews() {
        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup date range selector
        binding.chipToday.setOnClickListener {
            viewModel.setTimeRange("today")
            updateChartTitle("Today's Analytics")
        }
        binding.chipWeek.setOnClickListener {
            viewModel.setTimeRange("week")
            updateChartTitle("This Week's Analytics")
        }
        binding.chipMonth.setOnClickListener {
            viewModel.setTimeRange("month")
            updateChartTitle("This Month's Analytics")
        }

        // Setup export buttons
        binding.btnExportPdf.setOnClickListener { exportCurrentData() }
        binding.btnShareReport.setOnClickListener { shareCurrentData() }

        // Setup category correction button (for user feedback learning)
        binding.btnCorrectCategories.setOnClickListener { showCategoryCorrectionDialog() }

        // Setup RecyclerView for detailed stats
        detailedAdapter = DetailedStatsAdapter { onDetailedItemClick(it) }
        binding.recyclerViewDetailedStats.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewDetailedStats.adapter = detailedAdapter
    }

    private fun setupObservers() {
        viewModel.weeklySummaries.observe(this) { summaries ->
            lastSummaries = summaries
            updateWeeklyBarChart(summaries)
            refreshDetailedStats()
        }
        viewModel.categoryAnalytics.observe(this) { categories ->
            lastCategories = categories
            updateCategoryPieChart(categories)
            refreshDetailedStats()
        }
        viewModel.appAnalytics.observe(this) { apps ->
            lastApps = apps
            updateAppAnalytics(apps)
            updateTopAppsBarChart(apps)
            refreshDetailedStats()
        }
        viewModel.currentTimeRange.observe(this) { timeRange -> updateUIForTimeRange(timeRange) }
    }

    private fun refreshDetailedStats() {
        val days = lastSummaries.size.coerceAtLeast(1)
        val totalTime = lastSummaries.sumOf { it.totalScreenTimeMs }
        val totalSessions = lastSummaries.sumOf { it.sessionsCount }
        val avgPerDay = totalTime / days
        val busiest = lastSummaries.maxByOrNull { it.totalScreenTimeMs }
        val topCategory = lastCategories.maxByOrNull { it.totalTimeMs }
        val topApp = lastApps.maxByOrNull { it.totalTimeMs }
        val itemList = mutableListOf<DetailedStatItem>()

        itemList += DetailedStatItem("Average per day", formatDuration(avgPerDay))
        itemList += DetailedStatItem("Busiest day", busiest?.let { "${formatDisplayDate(it.date)} • ${formatDuration(it.totalScreenTimeMs)}" } ?: "-")
        itemList += DetailedStatItem("Sessions per day (avg)", String.format(Locale.getDefault(), "%.1f", totalSessions.toFloat() / days))
        itemList += DetailedStatItem("Top category", topCategory?.let { "${it.category} • ${formatDuration(it.totalTimeMs)}" } ?: "-")
        itemList += DetailedStatItem("Top app", topApp?.let { "${sanitizeAppName(it.appName, it.packageName)} • ${formatDuration(it.totalTimeMs)}" } ?: "-")
        itemList += DetailedStatItem("Apps used", lastApps.size.toString())
        itemList += DetailedStatItem("Categories covered", lastCategories.size.toString())

        detailedAdapter.submitList(itemList)
    }

    /** Pick a friendly Y tick step (in minutes) and configure the axis with headroom. */
    private fun configureYAxis(axis: YAxis, maxMinutes: Float) {
        val step = when {
            maxMinutes <= 60f -> 10f
            maxMinutes <= 120f -> 20f
            maxMinutes <= 240f -> 30f
            maxMinutes <= 360f -> 60f
            maxMinutes <= 720f -> 120f
            else -> 180f
        }
        val maxRounded = kotlin.math.ceil(maxMinutes / step) * step
        val ticks = kotlin.math.min(6, (maxRounded / step).toInt() + 1)
        axis.axisMinimum = 0f
        axis.axisMaximum = maxRounded.toFloat()
        axis.setLabelCount(ticks, true)
        axis.granularity = step
        axis.spaceTop = 0.12f
        axis.setDrawGridLines(true)
        axis.gridColor = Color.parseColor("#E6E6E6")
        axis.gridLineWidth = 0.7f
        axis.textColor = Color.parseColor("#666666")
        axis.axisLineColor = Color.parseColor("#CCCCCC")
        axis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String = formatMinutesLabel(value)
        }
    }

    private fun updateWeeklyBarChart(summaries: List<com.lohith.scrollsense.data.DailySummary>) {
        val entries = summaries.mapIndexed { index, summary ->
            BarEntry(index.toFloat(), (summary.totalScreenTimeMs / (1000 * 60)).toFloat())
        }
        val dataSet = BarDataSet(entries, "Screen Time").apply {
            colors = generateDistinctColors(entries.size)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() { override fun getFormattedValue(value: Float): String = formatMinutesLabel(value) }
        }
        val barData = BarData(dataSet).apply { barWidth = 0.6f }
        val maxMinutes = entries.maxOfOrNull { it.y } ?: 0f
        binding.barChartWeekly.apply {
            data = barData
            description.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setGranularityEnabled(true)
                setDrawGridLines(false)
                setAvoidFirstLastClipping(false)
                yOffset = 6f
                textSize = 10f
                axisMinimum = -0.5f
                axisMaximum = entries.size - 0.5f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index in entries.indices) {
                            when (viewModel.currentTimeRange.value) {
                                "month" -> "W${index + 1}"
                                else -> try {
                                    SimpleDateFormat("E", Locale.getDefault()).format(
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(summaries[index].date) ?: Date()
                                    )
                                } catch (_: Exception) { summaries[index].date }
                            }
                        } else ""
                    }
                }
            }
            axisRight.isEnabled = false
            configureYAxis(axisLeft, maxMinutes)
            legend.isEnabled = false
            setExtraOffsets(8f, 12f, 8f, 24f)
            animateY(1000)
            invalidate()
        }
    }

    private fun updateCategoryPieChart(categories: List<com.lohith.scrollsense.analytics.WeeklyCategoryData>) {
        val valid = categories.filter { it.totalTimeMs > 0 }
        if (valid.isEmpty()) {
            binding.pieChartCategories.clear(); binding.pieChartCategories.invalidate(); categoryLegendGroup?.removeAllViews(); return
        }
        val entries = valid.map { c -> PieEntry((c.totalTimeMs / (1000 * 60)).toFloat(), c.category) }
        val colors = generateDistinctColors(entries.size)
        val dataSet = PieDataSet(entries, "Categories").apply {
            this.colors = colors
            valueTextSize = 11f
            valueTextColor = Color.parseColor("#444444")
            sliceSpace = 2f
            setDrawValues(false) // turn off values on slices (safety on dataset)
        }
        binding.pieChartCategories.apply {
            val chartRef = this
            val pieData = PieData(dataSet).apply {
                setDrawValues(false) // and on PieData itself to fully disable any value rendering
            }
            data = pieData // no value formatter; values hidden
            description.isEnabled = false
            centerText = "Categories"
            setUsePercentValues(false) // keep percentages only in legend
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 62f
            legend.isEnabled = false
            setExtraOffsets(8f, 8f, 8f, 8f)
            animateY(800)
            invalidate()
        }
        // Build legend chip labels with percentages like in PDFs
        val total = valid.sumOf { it.totalTimeMs }.toFloat()
        val labelWithPct = valid.map { c ->
            val pct = if (total > 0f) (c.totalTimeMs / total) * 100f else 0f
            "${c.category} (${String.format(Locale.getDefault(),"%.0f%%", pct)})"
        }
        renderCategoryLegendChips(labelWithPct, colors)
    }

    private fun renderCategoryLegendChips(labels: List<String>, colors: List<Int>) {
        val container = binding.pieChartCategories.parent as? ViewGroup ?: return
        if (categoryLegendGroup == null) {
            categoryLegendGroup = ChipGroup(this).apply {
                isSingleLine = false
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (8 * resources.displayMetrics.density).toInt()
                }
            }
            val pieIndex = container.indexOfChild(binding.pieChartCategories)
            if (pieIndex >= 0) container.addView(categoryLegendGroup, pieIndex + 1) else container.addView(categoryLegendGroup)
        }
        val group = categoryLegendGroup ?: return
        group.removeAllViews()
        labels.forEachIndexed { i, label ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = false
                isClickable = false
                isChipIconVisible = true
                setChipIconResource(android.R.drawable.presence_online)
                chipIconTint = android.content.res.ColorStateList.valueOf(colors[i])
                textSize = if (labels.size > 10) 12f else 13f
            }
            group.addView(chip)
        }
    }

    private fun updateTopAppsBarChart(apps: List<com.lohith.scrollsense.analytics.WeeklyAppData>) {
        val topApps = apps.take(5)
        if (topApps.isEmpty()) { binding.barChartTopApps.clear(); binding.barChartTopApps.invalidate(); return }
        val entries = topApps.mapIndexed { index, app -> BarEntry(index.toFloat(), (app.totalTimeMs / (1000 * 60)).toFloat()) }
        val dataSet = BarDataSet(entries, "Usage Time").apply {
            colors = generateDistinctColors(entries.size)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() { override fun getFormattedValue(value: Float): String = formatMinutesLabel(value) }
        }
        val labels = topApps.map { sanitizeAppName(it.appName, it.packageName).take(24) }
        val avgLen = if (labels.isNotEmpty()) labels.sumOf { it.length }.toFloat() / labels.size else 0f
        // Tight bottom offset: small base + gentle slope; clamp to keep bars tall
        val bottomOffset = (16f + (avgLen - 8f).coerceAtLeast(0f) * 0.9f).coerceIn(16f, 30f)

        // Restore base height to keep plot area tall (no extra growth); rely on bottom offset only
        binding.barChartTopApps.post {
            val baseHeight = dpToPxInt(200f)
            val lp = binding.barChartTopApps.layoutParams
            if (lp.height != baseHeight) { lp.height = baseHeight; binding.barChartTopApps.layoutParams = lp }
        }

        val maxMinutes = entries.maxOfOrNull { it.y } ?: 0f
        binding.barChartTopApps.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f } // constant bar width
            description.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setGranularityEnabled(true)
                setDrawGridLines(false)
                setAvoidFirstLastClipping(false)
                labelRotationAngle = -40f
                yOffset = 6f
                textSize = 10f
                axisMinimum = -0.5f
                axisMaximum = entries.size - 0.5f
                setLabelCount(labels.size, false)
            }
            configureYAxis(axisLeft, maxMinutes)
            axisRight.isEnabled = false
            legend.isEnabled = false
            setExtraOffsets(8f, 12f, 8f, bottomOffset)
            animateY(900)
            invalidate()
        }
    }

    private fun onDetailedItemClick(item: DetailedStatItem) {
        when (item.title) {
            "Top category" -> {
                smoothScrollToView(binding.pieChartCategories)
                binding.pieChartCategories.animateY(600)
            }
            "Top app", "Apps used" -> {
                smoothScrollToView(binding.barChartTopApps)
                binding.barChartTopApps.animateY(600)
            }
            else -> {
                smoothScrollToView(binding.barChartWeekly)
                binding.barChartWeekly.animateY(600)
            }
        }
    }

    private fun smoothScrollToView(target: View) {
        var parent: View? = target
        var scrollView: NestedScrollView? = null
        repeat(8) {
            parent = (parent?.parent as? View) ?: return@repeat
            if (parent is NestedScrollView) { scrollView = parent as NestedScrollView; return@repeat }
        }
        scrollView?.let { ns ->
            val rect = Rect()
            target.getDrawingRect(rect)
            ns.offsetDescendantRectToMyCoords(target, rect)
            ns.smoothScrollTo(0, (rect.top - 24 * resources.displayMetrics.density).toInt())
        }
    }

    private fun updateAppAnalytics(apps: List<com.lohith.scrollsense.analytics.WeeklyAppData>) {
        // Update top apps summary
        val topAppsText = apps.take(5).joinToString("\n") { app ->
            "${sanitizeAppName(app.appName, app.packageName)}: ${formatDuration(app.totalTimeMs)}"
        }
        binding.textTopApps.text = topAppsText

        // Update total screen time
        val totalTime = apps.sumOf { it.totalTimeMs }
        binding.textTotalScreenTime.text = formatDuration(totalTime)

        // Update session count
        val totalSessions = apps.sumOf { it.totalSessions }
        binding.textTotalSessions.text = totalSessions.toString()
    }

    private fun updateUIForTimeRange(timeRange: String) {
        // Update chip selection
        binding.chipToday.isChecked = timeRange == "today"
        binding.chipWeek.isChecked = timeRange == "week"
        binding.chipMonth.isChecked = timeRange == "month"

        // Update trend chart visibility and title based on time range
        when (timeRange) {
            "today" -> {
                binding.cardTrendChart.visibility = android.view.View.GONE
                binding.textTrendTitle.text = "Daily Trend"
            }
            "week" -> {
                binding.cardTrendChart.visibility = android.view.View.VISIBLE
                binding.textTrendTitle.text = "Daily Trend"
            }
            "month" -> {
                binding.cardTrendChart.visibility = android.view.View.VISIBLE
                binding.textTrendTitle.text = "Weekly Trend"
            }
        }
    }

    private fun updateChartTitle(title: String) {
        binding.textChartTitle.text = title
    }

    private fun exportCurrentData() {
        lifecycleScope.launch {
            try {
                binding.progressExport.visibility = android.view.View.VISIBLE

                val timeRange = viewModel.currentTimeRange.value ?: "today"
                val file = when (timeRange) {
                    "today" -> {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val dailySummary = viewModel.getDailySummary(today)
                        val categoryAnalytics = viewModel.getDailyCategoryAnalytics(today)
                        val appAnalytics = viewModel.getDailyAppAnalytics(today)

                        if (dailySummary != null) {
                            pdfExporter.exportDailyReport(dailySummary, categoryAnalytics, appAnalytics)
                        } else {
                            showToast("No data available for today")
                            return@launch
                        }
                    }
                    else -> {
                        val weeklyAnalytics = viewModel.getCurrentWeeklyAnalytics()
                        if (weeklyAnalytics != null) {
                            pdfExporter.exportWeeklyReport(weeklyAnalytics)
                        } else {
                            showToast("No data available for the selected period")
                            return@launch
                        }
                    }
                }

                binding.progressExport.visibility = android.view.View.GONE
                showToast("Report exported: ${file.name}")

                // Open the PDF
                openPDF(file)

            } catch (e: Exception) {
                binding.progressExport.visibility = android.view.View.GONE
                showToast("Export failed: ${e.message}")
            }
        }
    }

    private fun shareCurrentData() {
        lifecycleScope.launch {
            try {
                val timeRange = viewModel.currentTimeRange.value ?: "today"
                val file = when (timeRange) {
                    "today" -> {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val dailySummary = viewModel.getDailySummary(today)
                        val categoryAnalytics = viewModel.getDailyCategoryAnalytics(today)
                        val appAnalytics = viewModel.getDailyAppAnalytics(today)

                        if (dailySummary != null) {
                            pdfExporter.exportDailyReport(dailySummary, categoryAnalytics, appAnalytics)
                        } else {
                            showToast("No data available for today")
                            return@launch
                        }
                    }
                    else -> {
                        val weeklyAnalytics = viewModel.getCurrentWeeklyAnalytics()
                        if (weeklyAnalytics != null) {
                            pdfExporter.exportWeeklyReport(weeklyAnalytics)
                        } else {
                            showToast("No data available for the selected period")
                            return@launch
                        }
                    }
                }

                sharePDF(file)

            } catch (e: Exception) {
                showToast("Share failed: ${e.message}")
            }
        }
    }

    private fun showCategoryCorrectionDialog() {
        showToast("Category correction feature coming soon!")
    }

    private fun openPDF(file: java.io.File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (_: Exception) {
            showToast("No PDF viewer found. File saved to: ${file.absolutePath}")
        }
    }

    private fun sharePDF(file: java.io.File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ScrollSense Usage Report")
                putExtra(Intent.EXTRA_TEXT, "Here's my screen time report from ScrollSense")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(shareIntent, "Share Report"))
        } catch (e: Exception) {
            showToast("Share failed: ${e.message}")
        }
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

    // Minute label formatter for charts to show Xh Ym when >=60m
    private fun formatMinutesLabel(valueInMinutes: Float): String {
        val totalMinutes = valueInMinutes.toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Replace system dialer/telecom packages with a friendly label; also normalize Chrome
    private fun sanitizeAppName(appName: String, packageName: String?): String {
        val key = ((packageName ?: "") + "|" + appName).lowercase(Locale.getDefault())
        return when {
            key.contains("incallui") || key.contains("dialer") || key.contains("telecom") -> "Call"
            key.contains("com.android.chrome") || key.contains("org.chromium.chrome") || key.contains(" chrome") || key.endsWith(".chrome") -> "Chrome"
            else -> appName
        }
    }

    private fun formatDisplayDate(dateStr: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dt = inFmt.parse(dateStr)
            SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(dt ?: Date())
        } catch (_: Exception) {
            dateStr
        }
    }

    private fun dpToPxInt(dp: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}
