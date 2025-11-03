package com.lohith.scrollsense.workers

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
// --- THIS IS THE FIX ---
// Import the correct object from the .util package
import com.lohith.scrollsense.util.PackageNameHelper
// --- END FIX ---
import com.lohith.scrollsense.analytics.AnalyticsProcessor
import com.lohith.scrollsense.util.NotificationHelper
import com.lohith.scrollsense.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class AnalyticsWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "AnalyticsWorker"
        private const val TAG = "AnalyticsWorker"
    }

    // Initialize helpers (PackageNameHelper is an object, so it doesn't need to be here)
    private val preferencesManager = PreferencesManager.get(context)
    private val analyticsProcessor = AnalyticsProcessor(context)


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "AnalyticsWorker started...")

            // 1. Process Analytics
            val calendar = Calendar.getInstance()
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            analyticsProcessor.processDayAnalytics(yesterday)
            analyticsProcessor.processDayAnalytics(today)
            Log.d(TAG, "Successfully processed analytics for $yesterday and $today")

            // 2. Check for App Limits
            Log.d(TAG, "Checking for app limits...")
            checkForAppLimits()

            Log.d(TAG, "AnalyticsWorker finished successfully.")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in AnalyticsWorker", e)
            Result.failure()
        }
    }

    private fun getTodayTimeRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()
        return Pair(startTime, endTime)
    }

    private fun checkForAppLimits() {
        val appLimits = preferencesManager.getParentalLimits()
        if (appLimits.isEmpty()) {
            Log.d(TAG, "No app limits set.")
            return
        }

        val notifiedToday = preferencesManager.getNotifiedApps()
        val (startTime, endTime) = getTodayTimeRange()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        val usageStatsMap = usageStatsList.associateBy { it.packageName }

        appLimits.forEach { (packageName, limitMinutes) ->
            val limitMillis = limitMinutes * 60 * 1000L
            val appUsageMillis = usageStatsMap[packageName]?.totalTimeInForeground ?: 0L

            if (appUsageMillis > limitMillis && packageName !in notifiedToday) {
                Log.i(TAG, "Limit exceeded for $packageName")

                // --- THIS IS THE FIX ---
                // Call the 'getAppLabel' method on the PackageNameHelper object
                val appName = PackageNameHelper.getAppLabel(context, packageName)
                // --- END FIX ---

                NotificationHelper.sendLimitNotification(
                    context,
                    appName,
                    packageName,
                    limitMinutes
                )

                preferencesManager.addNotifiedApp(packageName)
            }
        }
    }
}