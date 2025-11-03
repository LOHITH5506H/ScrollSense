package com.lohith.scrollsense.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.app.usage.UsageStats // --- NEW IMPORT ---
import android.app.usage.UsageStatsManager // --- NEW IMPORT ---
import android.content.Context // --- NEW IMPORT ---
// --- IMPORTS FIXED ---
import com.lohith.scrollsense.util.PackageNameHelper // Correct package (no .util)
// --- END FIX ---
import com.lohith.scrollsense.analytics.AnalyticsProcessor
import com.lohith.scrollsense.data.HybridUsageTracker
import com.lohith.scrollsense.util.NotificationHelper
import com.lohith.scrollsense.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*

class AnalyticsBackgroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var analyticsProcessor: AnalyticsProcessor
    private lateinit var hybridUsageTracker: HybridUsageTracker
    private lateinit var preferencesManager: PreferencesManager
    // --- NO LONGER NEEDED as properties, they are Objects (static)
    // private lateinit var usageStatsHelper: UsageStatsHelper
    // private lateinit var packageNameHelper: PackageNameHelper

    companion object {
        private const val TAG = "AnalyticsService"
    }

    override fun onCreate() {
        super.onCreate()
        analyticsProcessor = AnalyticsProcessor(this)
        hybridUsageTracker = HybridUsageTracker(this)
        preferencesManager = PreferencesManager.get(applicationContext)

        Log.d(TAG, "Analytics Background Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Analytics Background Service Started")

        serviceScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                analyticsProcessor.processDayAnalytics(yesterday)
                analyticsProcessor.processDayAnalytics(today)

                Log.d(TAG, "Checking for app limits...")
                checkForAppLimits()

                Log.d(TAG, "Successfully processed analytics for $yesterday and $today")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing daily analytics", e)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Analytics Background Service Destroyed")
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

        // --- LOGIC FIXED: Call UsageStatsManager directly ---
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        // Convert list to map for easy lookup
        val usageStatsMap = usageStatsList.associateBy { it.packageName }
        // --- END FIX ---

        appLimits.forEach { (packageName, limitMinutes) ->
            val limitMillis = limitMinutes * 60 * 1000L

            // --- LOGIC FIXED: Use the map we just created ---
            val appUsageMillis = usageStatsMap[packageName]?.totalTimeInForeground ?: 0L

            if (appUsageMillis > limitMillis && packageName !in notifiedToday) {
                Log.i(TAG, "Limit exceeded for $packageName")

                // --- FUNCTION NAME FIXED: Use getAppLabel (static) ---
                val appName = PackageNameHelper.getAppLabel(this, packageName)

                NotificationHelper.sendLimitNotification(
                    applicationContext,
                    appName,
                    packageName,
                    limitMinutes
                )

                preferencesManager.addNotifiedApp(packageName)
            }
        }
    }
}