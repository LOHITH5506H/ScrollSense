package com.lohith.scrollsense.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.lohith.scrollsense.adapters.AppUsageAdapter
import com.lohith.scrollsense.data.models.AppUsageData
import com.lohith.scrollsense.data.models.CategoryData
import com.lohith.scrollsense.databinding.FragmentDashboardBinding
import com.lohith.scrollsense.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit
import java.util.Locale
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // This is the correct way to get the shared ViewModel in a Fragment
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var appUsageAdapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBarChart()
        observeData()
    }

    private fun setupRecyclerView() {
        appUsageAdapter = AppUsageAdapter {
            // Handle app item clicks here if needed
        }
        binding.topAppsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = appUsageAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeData() {
        // Collect flows from MainViewModel in a lifecycle-aware way
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // App usage list -> adapter
                launch {
                    viewModel.appUsage.collect { usageList ->
                        val topApps = usageList
                            .sortedByDescending { it.totalDuration }
                            .take(5)
                            .map { app ->
                                // Map to AppUsageData for adapter display
                                AppUsageData(
                                    packageName = app.appName,
                                    appName = app.appName,
                                    category = "Utilities",
                                    totalTimeInForeground = app.totalDuration,
                                    lastTimeUsed = 0L,
                                    launchCount = 0
                                )
                            }
                        appUsageAdapter.updateData(topApps)
                        binding.topAppsCountText.text = String.format(Locale.getDefault(), "Showing %d apps", topApps.count())
                    }
                }
                // Category usage -> bar chart
                launch {
                    viewModel.categoryUsage.collect { categoryList ->
                        val categories = categoryList.map { c ->
                            CategoryData(
                                categoryName = c.categoryName,
                                totalTime = c.totalDuration
                            )
                        }
                        updateBarChart(categories)
                    }
                }
            }
        }
    }

    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.DKGRAY
            }
            axisLeft.apply {
                axisMinimum = 0f
                textColor = Color.DKGRAY
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(value.toLong())
                        return if (minutes < 60) "${minutes}m" else "${minutes / 60}h"
                    }
                }
            }
            axisRight.isEnabled = false
        }
    }

    private fun updateBarChart(categories: List<CategoryData>) {
        if (categories.isEmpty()) {
            binding.barChart.clear()
            binding.barChart.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()

        categories.forEachIndexed { index, category ->
            entries.add(BarEntry(index.toFloat(), category.totalTime.toFloat()))
            labels.add(category.categoryName)
            colors.add(Color.parseColor(category.color))
        }

        val dataSet = BarDataSet(entries, "Usage").apply {
            this.colors = colors
            valueTextSize = 10f
        }

        binding.barChart.data = BarData(dataSet).apply { barWidth = 0.5f }
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.barChart.xAxis.labelCount = labels.size
        binding.barChart.animateY(1000)
        binding.barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}