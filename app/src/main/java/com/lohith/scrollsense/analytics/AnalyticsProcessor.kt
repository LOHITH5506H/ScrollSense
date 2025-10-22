package com.lohith.scrollsense.analytics

import android.content.Context
import android.util.Log
import com.lohith.scrollsense.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Processes and stores daily analytics summaries
 * This runs automatically at the end of each day or when the app starts
 */
class AnalyticsProcessor(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val usageEventDao = database.usageEventDao()
    private val dailySummaryDao = database.dailySummaryDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val TAG = "AnalyticsProcessor"
    }

    /**
     * Process analytics for a specific date
     */
    suspend fun processDayAnalytics(date: String = getTodayString()) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing analytics for date: $date")

            // Check if we already processed this date
            val existingSummary = dailySummaryDao.getDailySummary(date)
            if (existingSummary != null && !isToday(date)) {
                Log.d(TAG, "Analytics already processed for $date")
                return@withContext
            }

            val startTime = getStartOfDay(date)
            val endTime = getEndOfDay(date)

            // Get all usage events for the day
            val dayEvents = usageEventDao.getEventsBetween(startTime, endTime)

            if (dayEvents.isEmpty()) {
                Log.d(TAG, "No events found for $date")
                return@withContext
            }

            // Calculate daily summary
            val totalScreenTime = dayEvents.sumOf { it.durationMs }
            val sessionsCount = dayEvents.size
            val averageSessionDuration = if (sessionsCount > 0) totalScreenTime / sessionsCount else 0L

            // Find top category and app
            val categoryUsage = dayEvents.groupBy { it.category }
                .mapValues { (_, events) -> events.sumOf { it.durationMs } }
            val topCategory = categoryUsage.maxByOrNull { it.value }?.key ?: "other"

            val appUsage = dayEvents.groupBy { it.appLabel }
                .mapValues { (_, events) -> events.sumOf { it.durationMs } }
            val topApp = appUsage.maxByOrNull { it.value }?.key ?: "Unknown"

            // Store daily summary
            val dailySummary = DailySummary(
                date = date,
                totalScreenTimeMs = totalScreenTime,
                topCategory = topCategory,
                topApp = topApp,
                sessionsCount = sessionsCount,
                averageSessionDurationMs = averageSessionDuration
            )
            dailySummaryDao.insertDailySummary(dailySummary)

            // Store category analytics
            val categoryAnalytics = categoryUsage.map { (category, timeMs) ->
                DailyCategoryAnalytics(
                    date = date,
                    category = category,
                    totalTimeMs = timeMs,
                    sessionsCount = dayEvents.count { it.category == category },
                    percentageOfDay = if (totalScreenTime > 0) (timeMs.toFloat() / totalScreenTime) * 100f else 0f
                )
            }
            dailySummaryDao.insertCategoryAnalytics(categoryAnalytics)

            // Store app analytics
            val appAnalytics = dayEvents.groupBy { it.packageName to it.appLabel }
                .map { (appInfo, events) ->
                    val timeMs = events.sumOf { it.durationMs }
                    DailyAppAnalytics(
                        date = date,
                        packageName = appInfo.first,
                        appName = appInfo.second,
                        category = events.first().category,
                        totalTimeMs = timeMs,
                        sessionsCount = events.size,
                        percentageOfDay = if (totalScreenTime > 0) (timeMs.toFloat() / totalScreenTime) * 100f else 0f
                    )
                }
            dailySummaryDao.insertAppAnalytics(appAnalytics)

            Log.d(TAG, "Successfully processed analytics for $date - Total time: ${totalScreenTime}ms, Sessions: $sessionsCount")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing day analytics for $date", e)
        }
    }

    /**
     * Process analytics for the last [days] days (inclusive of today)
     */
    suspend fun processLastDays(days: Int) = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        repeat(days) { dayOffset ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
            val date = dateFormat.format(calendar.time)
            processDayAnalytics(date)
        }
    }

    /**
     * Process analytics for the last 7 days (for catching up)
     */
    suspend fun processLast7Days() = processLastDays(7)

    /**
     * Get aggregated analytics for a date range (for weekly views)
     */
    suspend fun getWeeklyAnalytics(startDate: String, endDate: String): WeeklyAnalytics = withContext(Dispatchers.IO) {
        val summaries = dailySummaryDao.getDailySummaries(startDate, endDate)
        val categoryAnalytics = dailySummaryDao.getCategoryAnalyticsRange(startDate, endDate)
        val appAnalytics = dailySummaryDao.getAppAnalyticsRange(startDate, endDate)

        // Aggregate weekly data
        val totalScreenTime = summaries.sumOf { it.totalScreenTimeMs }
        val totalSessions = summaries.sumOf { it.sessionsCount }
        val averageSessionDuration = if (totalSessions > 0) totalScreenTime / totalSessions else 0L

        // Aggregate categories
        val weeklyCategories = categoryAnalytics.groupBy { it.category }
            .map { (category, analytics) ->
                WeeklyCategoryData(
                    category = category,
                    totalTimeMs = analytics.sumOf { it.totalTimeMs },
                    totalSessions = analytics.sumOf { it.sessionsCount },
                    averagePerDay = analytics.sumOf { it.totalTimeMs } / summaries.size.coerceAtLeast(1)
                )
            }.sortedByDescending { it.totalTimeMs }

        // Aggregate apps
        val weeklyApps = appAnalytics.groupBy { it.packageName }
            .map { (packageName, analytics) ->
                WeeklyAppData(
                    packageName = packageName,
                    appName = analytics.first().appName,
                    category = analytics.first().category,
                    totalTimeMs = analytics.sumOf { it.totalTimeMs },
                    totalSessions = analytics.sumOf { it.sessionsCount },
                    averagePerDay = analytics.sumOf { it.totalTimeMs } / summaries.size.coerceAtLeast(1)
                )
            }.sortedByDescending { it.totalTimeMs }

        WeeklyAnalytics(
            startDate = startDate,
            endDate = endDate,
            totalScreenTimeMs = totalScreenTime,
            totalSessions = totalSessions,
            averageSessionDurationMs = averageSessionDuration,
            dailySummaries = summaries,
            categoryData = weeklyCategories,
            appData = weeklyApps
        )
    }

    private fun getTodayString(): String {
        return dateFormat.format(Date())
    }

    private fun isToday(date: String): Boolean {
        return date == getTodayString()
    }

    private fun getStartOfDay(date: String): Long {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(date) ?: Date()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(date: String): Long {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(date) ?: Date()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}

// Data classes for weekly analytics
data class WeeklyAnalytics(
    val startDate: String,
    val endDate: String,
    val totalScreenTimeMs: Long,
    val totalSessions: Int,
    val averageSessionDurationMs: Long,
    val dailySummaries: List<DailySummary>,
    val categoryData: List<WeeklyCategoryData>,
    val appData: List<WeeklyAppData>
)

data class WeeklyCategoryData(
    val category: String,
    val totalTimeMs: Long,
    val totalSessions: Int,
    val averagePerDay: Long
)

data class WeeklyAppData(
    val packageName: String,
    val appName: String,
    val category: String,
    val totalTimeMs: Long,
    val totalSessions: Int,
    val averagePerDay: Long
)
