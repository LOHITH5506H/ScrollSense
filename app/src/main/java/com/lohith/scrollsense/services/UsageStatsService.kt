package com.lohith.scrollsense.services

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.lohith.scrollsense.data.models.AppUsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class UsageStatsService(private val context: Context) {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val packageManager: PackageManager = context.packageManager

    companion object {
        private const val TAG = "UsageStatsService"

        // Comprehensive list of system packages to exclude
        private val SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.android.settings",
            "com.android.inputmethod",
            "android.process.acore",
            "com.android.phone",
            "com.android.dialer",
            "com.google.android.googlequicksearchbox",
            "com.android.vending",
            "com.google.android.gms",
            "com.android.keyguard",
            "com.android.wallpaper",
            "com.android.permissioncontroller",
            "com.google.android.apps.nexuslauncher",
            "com.oneplus.launcher",
            "com.samsung.android.launcher",
            "com.miui.home",
            "com.huawei.android.launcher"
        )

        // Package prefixes to exclude
        private val SYSTEM_PACKAGE_PREFIXES = setOf(
            "com.android.",
            "com.google.android.gms",
            "android.",
            "com.sec.android.",
            "com.samsung.android.app.",
            "com.miui.",
            "com.oneplus.android.",
            "com.huawei.android."
        )
    }

    private fun shouldExcludePackage(packageName: String, totalTimeInForeground: Long): Boolean {
        if (SYSTEM_PACKAGES.contains(packageName)) return true
        if (SYSTEM_PACKAGE_PREFIXES.any { packageName.startsWith(it) }) return true
        if (totalTimeInForeground <= 0) return true
        if (packageName.contains("systemui", true) ||
            packageName.contains("launcher", true) ||
            packageName.contains("keyboard", true) ||
            packageName.contains("wallpaper", true)) return true
        return false
    }

    suspend fun getAppUsageStats(days: Int): List<AppUsageData> = withContext(Dispatchers.IO) {
        val appUsageList = mutableListOf<AppUsageData>()

        usageStatsManager ?: run {
            Log.e(TAG, "UsageStatsManager is null")
            return@withContext appUsageList
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        try {
            // Get usage stats for the specified period
            val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(
                startTime, endTime
            )

            for (usageStats in usageStatsMap.values) {
                val packageName = usageStats.packageName

                if (shouldExcludePackage(packageName, usageStats.totalTimeInForeground)) continue

                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()

                    // Additional system app check
                    if (isSystemApp(appInfo) && !isUserInstalledSystemApp(appInfo)) continue
                    // Only include apps with significant usage (more than 30 seconds)
                    if (usageStats.totalTimeInForeground < 30000) continue

                    val launchCount = getLaunchCount(packageName, startTime, endTime)

                    appUsageList.add(
                        AppUsageData(
                            packageName = packageName,
                            appName = appName,
                            totalTimeInForeground = usageStats.totalTimeInForeground,
                            lastTimeUsed = usageStats.lastTimeUsed,
                            launchCount = launchCount,
                            isSystemApp = isSystemApp(appInfo)
                        )
                    )

                } catch (_: PackageManager.NameNotFoundException) { }
            }

            // Sort by usage time (descending) and filter out negligible usage
            appUsageList.sortByDescending { it.totalTimeInForeground }

            Log.d(TAG, "Retrieved ${appUsageList.size} apps with significant usage")

        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving usage stats", e)
        }

        return@withContext appUsageList
    }

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    private fun isUserInstalledSystemApp(appInfo: ApplicationInfo): Boolean {
        // Check if it's a system app that user actively uses (like Chrome, Gmail etc.)
        return (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun getLaunchCount(packageName: String, startTime: Long, endTime: Long): Int {
        val usageEvents = usageStatsManager?.queryEvents(startTime, endTime)
        var launchCount = 0

        usageEvents?.let { events ->
            while (events.hasNextEvent()) {
                val event = UsageEvents.Event()
                events.getNextEvent(event)

                if (event.packageName == packageName &&
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    launchCount++
                }
            }
        }

        return launchCount
    }

    suspend fun getTodayUsageStats(): List<AppUsageData> = getAppUsageStats(1)

    suspend fun getWeeklyUsageStats(): List<AppUsageData> = getAppUsageStats(7)

    suspend fun getMonthlyUsageStats(): List<AppUsageData> = getAppUsageStats(30)

    suspend fun getTotalScreenTime(days: Int): Long = withContext(Dispatchers.IO) {
        val appUsageList = getAppUsageStats(days)
        return@withContext appUsageList.sumOf { it.totalTimeInForeground }
    }

    suspend fun getHourlyUsageStats(days: Int): Map<String, Long> = withContext(Dispatchers.IO) {
        val hourlyStats = mutableMapOf<String, Long>().apply { for (i in 0..23) put(String.format("%02d:00", i), 0L) }

        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageEvents = usageStatsManager?.queryEvents(startTime, endTime)
        val sessionStart = mutableMapOf<String, Long>()

        usageEvents?.let { events ->
            while (events.hasNextEvent()) {
                val event = UsageEvents.Event()
                events.getNextEvent(event)

                val packageName = event.packageName

                if (shouldExcludePackage(packageName, 1L)) continue

                val hourKey = String.format("%02d:00", Calendar.getInstance().apply { timeInMillis = event.timeStamp }.get(Calendar.HOUR_OF_DAY))

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> sessionStart[packageName] = event.timeStamp
                    UsageEvents.Event.ACTIVITY_PAUSED -> sessionStart[packageName]?.let { st ->
                        val duration = event.timeStamp - st
                        hourlyStats[hourKey] = (hourlyStats[hourKey] ?: 0L) + duration
                        sessionStart.remove(packageName)
                    }
                }
            }
        }

        return@withContext hourlyStats
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val usageStatsMap = usageStatsManager?.queryAndAggregateUsageStats(
                startTime, endTime
            )

            usageStatsMap != null && usageStatsMap.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage stats permission", e)
            false
        }
    }
}