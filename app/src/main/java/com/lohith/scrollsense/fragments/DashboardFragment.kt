package com.lohith.scrollsense.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.PercentFormatter
import com.lohith.scrollsense.MainActivity
import com.lohith.scrollsense.R
import com.lohith.scrollsense.adapters.AppUsageAdapter
import com.lohith.scrollsense.data.models.AppUsageData
import com.lohith.scrollsense.data.models.CategoryData
import com.lohith.scrollsense.databinding.FragmentDashboardBinding
import com.lohith.scrollsense.utils.AppCategorizer
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var appUsageAdapter: AppUsageAdapter
    private var currentAppUsageData: List<AppUsageData> = emptyList()
    private var currentCategoryData: List<CategoryData> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCharts()
        updateChartsWindowLabel()
        observeData()
    }

    private fun setupRecyclerView() {
        appUsageAdapter = AppUsageAdapter { app ->
            // Handle app item click
            showAppDetails(app)
        }

        binding.topAppsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = appUsageAdapter
            isNestedScrollingEnabled = false
            // Ensure last items are not obscured by the sticky bottom nav
            clipToPadding = false
            val extra = (16 * resources.displayMetrics.density).toInt() * 2 // ~32dp
            updatePadding(bottom = paddingBottom + extra)
        }
    }

    private fun setupCharts() {
        setupPieChart()
        setupBarChart()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            setUsePercentValues(true)
            description = Description().apply { isEnabled = false }
            setExtraOffsets(5f, 10f, 5f, 5f)

            dragDecelerationFrictionCoef = 0.95f

            setDrawHoleEnabled(true)
            setHoleColor(Color.TRANSPARENT)
            setHoleRadius(40f)
            setTransparentCircleRadius(45f)

            setDrawCenterText(true)
            setCenterText("App\nCategories")
            setCenterTextSize(16f)
            setCenterTextColor(Color.BLACK)

            setRotationAngle(0f)
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            // Legend configuration
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textSize = 12f
                isWordWrapEnabled = true
                maxSizePercent = 0.5f
            }

            // Animation
            animateY(1400, Easing.EaseInOutQuad)
        }
    }

    private fun setupBarChart() {
        binding.barChart.apply {
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setMaxVisibleValueCount(60)
            setPinchZoom(false)
            setDrawGridBackground(false)
            description = Description().apply { isEnabled = false }

            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = -45f
                textSize = 10f
                setLabelCount(8, false)
            }

            // Left Y-axis configuration
            axisLeft.apply {
                setLabelCount(6, false)
                valueFormatter = TimeAxisValueFormatter()
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                spaceTop = 15f
                axisMinimum = 0f
                textSize = 10f
            }

            // Right Y-axis (disabled)
            axisRight.isEnabled = false

            // Legend configuration
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(true)
                textSize = 10f
                isEnabled = false // Disable for cleaner look
            }

            // Animation
            animateY(1400, Easing.EaseInOutQuad)
        }
    }

    private fun observeData() {
        val mainActivity = activity as? MainActivity ?: return
        val viewModel = mainActivity.getViewModel()

        // Observe app usage data
        viewModel.appUsageData.observe(viewLifecycleOwner) { appUsageData ->
            currentAppUsageData = appUsageData
            updateTopAppsList()
        }

        // Observe category data
        viewModel.categoryData.observe(viewLifecycleOwner) { categoryData ->
            currentCategoryData = categoryData
            updateCharts()
            updateInsights()
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun updateCharts() {
        lifecycleScope.launch {
            if (currentCategoryData.isEmpty()) {
                showNoDataState()
                return@launch
            }

            hideNoDataState()
            updatePieChart()
            updateBarChart()
        }
    }

    private fun updatePieChart() {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        // Filter and sort categories by usage time
        val significantCategories = currentCategoryData
            .filter { it.hasSignificantUsage() }
            .sortedByDescending { it.totalTime }
            .take(8) // Limit to top 8 categories for better visualization

        for (category in significantCategories) {
            val hours = category.getUsageHours()
            if (hours > 0.01f) { // Only include categories with > 1 minute
                entries.add(PieEntry(hours, category.categoryName))
                colors.add(Color.parseColor(category.color))
            }
        }

        if (entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, "").apply {
                setColors(colors)
                valueTextColor = Color.BLACK
                valueTextSize = 11f
                sliceSpace = 2f
                selectionShift = 8f
            }

            val pieData = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter(binding.pieChart))
            }
            binding.pieChart.data = pieData
            binding.pieChart.highlightValues(null)
            binding.pieChart.invalidate()
        } else {
            binding.pieChart.clear()
        }
    }

    private fun updateBarChart() {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()

        // Get top categories for bar chart
        val topCategories = currentCategoryData
            .filter { it.hasSignificantUsage() }
            .sortedByDescending { it.totalTime }
            .take(8)

        topCategories.forEachIndexed { index, category ->
            val hours = category.getUsageHours()
            entries.add(BarEntry(index.toFloat(), hours))
            labels.add(category.categoryName)
            colors.add(Color.parseColor(category.color))
        }

        if (entries.isNotEmpty()) {
            val dataSet = BarDataSet(entries, "Usage Time").apply {
                setColors(colors)
                valueTextColor = Color.BLACK
                valueTextSize = 10f

                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value < 0.1f) {
                            "${(value * 60).toInt()}m"
                        } else {
                            "${value.format(1)}h"
                        }
                    }
                }
            }

            val barData = BarData(dataSet).apply {
                barWidth = 0.6f
            }

            binding.barChart.apply {
                data = barData
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.setLabelCount(labels.size, false)
                fitScreen()
                invalidate()
            }
        } else {
            binding.barChart.clear()
        }
    }

    private fun updateTopAppsList() {
        val topApps = currentAppUsageData
            .filter { it.isSignificantUsage() }
            .sortedByDescending { it.totalTimeInForeground }
            .take(10)

        appUsageAdapter.updateData(topApps)

        // Update apps count text
        binding.topAppsCountText.text = "Showing ${topApps.size} apps"
    }

    private fun updateInsights() {
        if (currentCategoryData.isEmpty()) {
            binding.insightText.text = "No insights available."
            return
        }

        val insights = AppCategorizer.getUsageInsights(currentCategoryData)
        binding.insightText.text = insights.joinToString("\n\n")
    }

    private fun showNoDataState() {
        binding.noDataLayout.visibility = View.VISIBLE
        binding.chartsLayout.visibility = View.GONE
    }

    private fun hideNoDataState() {
        binding.noDataLayout.visibility = View.GONE
        binding.chartsLayout.visibility = View.VISIBLE
    }

    private fun showAppDetails(app: AppUsageData) {
        // TODO: Navigate to app details screen or show bottom sheet
        val message = """
            App: ${app.appName}
            Category: ${app.category}
            Usage Time: ${app.getFormattedTime()}
            Launch Count: ${app.launchCount}
            Last Used: ${app.getFormattedLastUsed()}
        """.trimIndent()

        // For now, show a simple dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("App Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateChartsWindowLabel() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = Date(cal.timeInMillis)
        val now = Date()
        val tf = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
        binding.chartsSinceText.text = "Today: ${tf.format(start)} – ${tf.format(now)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Extension function for formatting doubles
    private fun Float.format(digits: Int): String = "%.${digits}f".format(this)
}

// Custom formatter for time values on Y-axis
class TimeAxisValueFormatter : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return if (value < 1f) {
            "${(value * 60f).roundToInt()}m"
        } else {
            String.format(Locale.getDefault(), "%.1fh", value)
        }
    }
}