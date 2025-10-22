package com.lohith.scrollsense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lohith.scrollsense.analytics.AnalyticsProcessor
import com.lohith.scrollsense.analytics.WeeklyAnalytics
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.DailyAppAnalytics
import com.lohith.scrollsense.data.DailyCategoryAnalytics
import com.lohith.scrollsense.data.DailySummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EnhancedAnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val analyticsProcessor = AnalyticsProcessor(application)
    private val dailySummaryDao = AppDatabase.getDatabase(application).dailySummaryDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _currentTimeRange = MutableLiveData("today")
    val currentTimeRange: LiveData<String> = _currentTimeRange

    private val _weeklySummaries = MutableLiveData<List<DailySummary>>()
    val weeklySummaries: LiveData<List<DailySummary>> = _weeklySummaries

    private val _categoryAnalytics = MutableLiveData<List<com.lohith.scrollsense.analytics.WeeklyCategoryData>>()
    val categoryAnalytics: LiveData<List<com.lohith.scrollsense.analytics.WeeklyCategoryData>> = _categoryAnalytics

    private val _appAnalytics = MutableLiveData<List<com.lohith.scrollsense.analytics.WeeklyAppData>>()
    val appAnalytics: LiveData<List<com.lohith.scrollsense.analytics.WeeklyAppData>> = _appAnalytics

    private var currentWeeklyAnalytics: WeeklyAnalytics? = null

    init {
        refreshData()
    }

    fun setTimeRange(timeRange: String) {
        _currentTimeRange.value = timeRange
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            val timeRange = _currentTimeRange.value ?: "today"

            when (timeRange) {
                "today" -> loadTodayData()
                "week" -> loadWeekData()
                "month" -> loadMonthData()
            }
        }
    }

    private suspend fun loadTodayData() {
        val today = dateFormat.format(Date())
        val dailySummary = dailySummaryDao.getDailySummary(today)

        if (dailySummary != null) {
            _weeklySummaries.postValue(listOf(dailySummary))

            val categoryAnalytics = dailySummaryDao.getDailyCategoryAnalytics(today)
            val weeklyCategories = categoryAnalytics.map { category ->
                com.lohith.scrollsense.analytics.WeeklyCategoryData(
                    category = category.category,
                    totalTimeMs = category.totalTimeMs,
                    totalSessions = category.sessionsCount,
                    averagePerDay = category.totalTimeMs
                )
            }
            _categoryAnalytics.postValue(weeklyCategories)

            val appAnalytics = dailySummaryDao.getDailyAppAnalytics(today)
            val weeklyApps = appAnalytics.map { app ->
                com.lohith.scrollsense.analytics.WeeklyAppData(
                    packageName = app.packageName,
                    appName = app.appName,
                    category = app.category,
                    totalTimeMs = app.totalTimeMs,
                    totalSessions = app.sessionsCount,
                    averagePerDay = app.totalTimeMs
                )
            }
            _appAnalytics.postValue(weeklyApps)
        }
    }

    private suspend fun loadWeekData() {
        val endDate = dateFormat.format(Date())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = dateFormat.format(calendar.time)

        val weeklyAnalytics = analyticsProcessor.getWeeklyAnalytics(startDate, endDate)
        currentWeeklyAnalytics = weeklyAnalytics

        _weeklySummaries.postValue(weeklyAnalytics.dailySummaries)
        _categoryAnalytics.postValue(weeklyAnalytics.categoryData)
        _appAnalytics.postValue(weeklyAnalytics.appData)
    }

    private suspend fun loadMonthData() {
        val endDate = dateFormat.format(Date())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -29)
        val startDate = dateFormat.format(calendar.time)

        try {
            val monthlyAnalytics = analyticsProcessor.getWeeklyAnalytics(startDate, endDate)
            currentWeeklyAnalytics = monthlyAnalytics

            // For monthly view, we'll show weekly summaries
            val weeklySummaries = groupDailySummariesByWeek(monthlyAnalytics.dailySummaries)
            _weeklySummaries.postValue(weeklySummaries)
            _categoryAnalytics.postValue(monthlyAnalytics.categoryData)
            _appAnalytics.postValue(monthlyAnalytics.appData)
        } catch (e: Exception) {
            // Fallback to empty data if processing fails
            _weeklySummaries.postValue(emptyList())
            _categoryAnalytics.postValue(emptyList())
            _appAnalytics.postValue(emptyList())
        }
    }

    private fun groupDailySummariesByWeek(dailySummaries: List<DailySummary>): List<DailySummary> {
        if (dailySummaries.isEmpty()) return emptyList()

        // Sort by date ascending so buckets are W1..Wn chronologically
        val sorted = dailySummaries.sortedBy { it.date }

        return sorted.chunked(7).mapIndexed { index, weekSummaries ->
            if (weekSummaries.isEmpty()) return@mapIndexed null

            val totalScreenTime = weekSummaries.sumOf { it.totalScreenTimeMs }
            val totalSessions = weekSummaries.sumOf { it.sessionsCount }
            val averageSessionDuration = if (totalSessions > 0) totalScreenTime / totalSessions else 0L

            DailySummary(
                date = "W${index + 1}",
                totalScreenTimeMs = totalScreenTime,
                topCategory = weekSummaries.groupBy { it.topCategory }
                    .maxByOrNull { it.value.size }?.key ?: "other",
                topApp = weekSummaries.groupBy { it.topApp }
                    .maxByOrNull { it.value.size }?.key ?: "Unknown",
                sessionsCount = totalSessions,
                averageSessionDurationMs = averageSessionDuration
            )
        }.filterNotNull()
    }

    suspend fun getDailySummary(date: String): DailySummary? {
        return dailySummaryDao.getDailySummary(date)
    }

    suspend fun getDailyCategoryAnalytics(date: String): List<DailyCategoryAnalytics> {
        return dailySummaryDao.getDailyCategoryAnalytics(date)
    }

    suspend fun getDailyAppAnalytics(date: String): List<DailyAppAnalytics> {
        return dailySummaryDao.getDailyAppAnalytics(date)
    }

    fun getCurrentWeeklyAnalytics(): WeeklyAnalytics? {
        return currentWeeklyAnalytics
    }
}
