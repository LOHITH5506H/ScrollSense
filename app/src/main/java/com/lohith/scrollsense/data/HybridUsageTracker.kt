package com.lohith.scrollsense.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Hybrid usage tracker that combines AccessibilityService data with UsageStatsManager
 * for more accurate screen time calculations that align with Digital Wellbeing
 */
class HybridUsageTracker(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    suspend fun getAccurateUsageStats(startTime: Long, endTime: Long): List<AccurateAppUsage> = withContext(Dispatchers.IO) {
        try {
            // Get system usage stats
            val systemStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            // Get our detailed accessibility data
            val accessibilityDao = AppDatabase.getDatabase(context).usageEventDao()
            val accessibilityEvents = accessibilityDao.getEventsBetween(startTime, endTime)

            // Combine and reconcile the data
            reconcileUsageData(systemStats, accessibilityEvents)
        } catch (e: Exception) {
            Log.e("HybridUsageTracker", "Error getting accurate usage stats", e)
            emptyList()
        }
    }

    private fun reconcileUsageData(
        systemStats: List<UsageStats>,
        accessibilityEvents: List<UsageEvent>
    ): List<AccurateAppUsage> {
        val systemStatsMap = systemStats.associateBy { it.packageName }
        val accessibilityStatsMap = accessibilityEvents
            .groupBy { it.packageName }
            .mapValues { (_, events) -> events.sumOf { it.durationMs } }

        val allPackages = (systemStatsMap.keys + accessibilityStatsMap.keys).distinct()

        return allPackages.mapNotNull { packageName ->
            val systemTime = systemStatsMap[packageName]?.totalTimeInForeground ?: 0L
            val accessibilityTime = accessibilityStatsMap[packageName] ?: 0L

            // Use system time as primary source, but fallback to accessibility data
            val finalTime = if (systemTime > 0) {
                // If we have both, use weighted average favoring system time
                if (accessibilityTime > 0) {
                    (systemTime * 0.7 + accessibilityTime * 0.3).toLong()
                } else {
                    systemTime
                }
            } else {
                accessibilityTime
            }

            if (finalTime > 0) {
                try {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    val appName = context.packageManager.getApplicationLabel(appInfo).toString()

                    AccurateAppUsage(
                        packageName = packageName,
                        appName = appName,
                        totalTimeMs = finalTime,
                        systemTimeMs = systemTime,
                        accessibilityTimeMs = accessibilityTime,
                        confidence = calculateConfidence(systemTime, accessibilityTime)
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }.sortedByDescending { it.totalTimeMs }
    }

    private fun calculateConfidence(systemTime: Long, accessibilityTime: Long): Float {
        return when {
            systemTime > 0 && accessibilityTime > 0 -> {
                val ratio = minOf(systemTime, accessibilityTime).toFloat() / maxOf(systemTime, accessibilityTime)
                ratio * 0.9f + 0.1f // Min confidence of 0.1
            }
            systemTime > 0 -> 0.8f // System time is generally reliable
            accessibilityTime > 0 -> 0.6f // Accessibility time is less reliable
            else -> 0.0f
        }
    }
}

data class AccurateAppUsage(
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long,
    val systemTimeMs: Long,
    val accessibilityTimeMs: Long,
    val confidence: Float
)
