package com.lohith.scrollsense.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CategoryData(
    val categoryName: String,
    var totalTime: Long = 0L,
    val apps: MutableList<AppUsageData> = mutableListOf(),
    var color: String = getDefaultColorForCategory(categoryName)
) : Parcelable {

    fun addApp(app: AppUsageData) {
        apps.add(app)
        totalTime += app.totalTimeInForeground
    }

    fun removeApp(app: AppUsageData) {
        apps.remove(app)
        calculateTotalTime()
    }

    fun calculateTotalTime() {
        totalTime = apps.sumOf { it.totalTimeInForeground }
    }

    fun getFormattedTime(): String {
        val hours = totalTime / 3600000
        val minutes = (totalTime % 3600000) / 60000

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${totalTime / 1000}s"
        }
    }

    fun getUsagePercentage(totalUsage: Long): Double {
        if (totalUsage == 0L) return 0.0
        return ((totalTime.toDouble() / totalUsage) * 100)
    }

    fun getAppCount(): Int = apps.size

    fun getTopApps(limit: Int = 5): List<AppUsageData> {
        return apps.sortedByDescending { it.totalTimeInForeground }.take(limit)
    }

    fun getUsageHours(): Float {
        return (totalTime / 3600000f)
    }

    fun hasSignificantUsage(): Boolean {
        return totalTime >= 300000 // At least 5 minutes
    }

    companion object {
        fun getDefaultColorForCategory(category: String): String {
            return when (category.lowercase()) {
                "social", "social media" -> "#E91E63" // Pink
                "entertainment", "video" -> "#F44336" // Red
                "games", "gaming" -> "#9C27B0" // Purple
                "productivity", "work" -> "#2196F3" // Blue
                "education", "learning" -> "#4CAF50" // Green
                "shopping" -> "#FF9800" // Orange
                "news", "reading", "news & reading" -> "#795548" // Brown
                "health", "fitness", "health & fitness" -> "#009688" // Teal
                "music", "audio" -> "#607D8B" // Blue Grey
                "photography", "photo" -> "#FFC107" // Amber
                "travel", "navigation" -> "#3F51B5" // Indigo
                "communication", "messaging" -> "#00BCD4" // Cyan
                "finance" -> "#8BC34A" // Light Green
                "food", "food & drink" -> "#FF5722" // Deep Orange
                "lifestyle" -> "#E91E63" // Pink
                "business" -> "#37474F" // Blue Grey
                "sports" -> "#FF9800" // Orange
                "weather" -> "#03A9F4" // Light Blue
                "utilities" -> "#9E9E9E" // Grey
                else -> "#9E9E9E" // Default Grey
            }
        }
    }
}