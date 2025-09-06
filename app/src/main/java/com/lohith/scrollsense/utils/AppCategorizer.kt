package com.lohith.scrollsense.utils

import com.lohith.scrollsense.data.models.AppUsageData
import com.lohith.scrollsense.data.models.CategoryData
import kotlin.math.max
import kotlin.math.min

object AppCategorizer {

    fun categorizeApps(appUsageList: List<AppUsageData>): List<CategoryData> {
        val categoryMap = mutableMapOf<String, CategoryData>()

        // Group apps by category
        appUsageList.forEach { app ->
            val categoryName = app.category
            val categoryData = categoryMap.getOrPut(categoryName) {
                CategoryData(categoryName = categoryName)
            }
            categoryData.addApp(app)
        }

        // Sort categories by total usage time (descending)
        return categoryMap.values
            .filter { it.hasSignificantUsage() }
            .sortedByDescending { it.totalTime }
    }

    fun getDetailedCategoryStats(categories: List<CategoryData>): Map<String, CategoryStats> {
        val totalScreenTime = categories.sumOf { it.totalTime }

        return categories.associate { category ->
            category.categoryName to CategoryStats(
                categoryName = category.categoryName,
                totalTime = category.totalTime,
                appCount = category.getAppCount(),
                percentage = if (totalScreenTime > 0) {
                    (category.totalTime.toDouble() / totalScreenTime) * 100
                } else 0.0,
                topApps = category.getTopApps(5),
                averageSessionTime = if (category.getAppCount() > 0) {
                    category.totalTime / category.getAppCount()
                } else 0L,
                color = category.color
            )
        }
    }

    fun getAppSpecificCategoryStats(
        categories: List<CategoryData>
    ): Map<String, List<AppCategoryStats>> {
        return categories.associate { category ->
            category.categoryName to category.apps.map { app ->
                AppCategoryStats(
                    appName = app.appName,
                    packageName = app.packageName,
                    timeInCategory = app.totalTimeInForeground,
                    categoryName = category.categoryName,
                    percentageInCategory = if (category.totalTime > 0) {
                        (app.totalTimeInForeground.toDouble() / category.totalTime) * 100
                    } else 0.0,
                    launchCount = app.launchCount
                )
            }.sortedByDescending { it.timeInCategory }
        }
    }

    fun getTotalUsageTime(categories: List<CategoryData>): Long {
        return categories.sumOf { it.totalTime }
    }

    fun getProductivityScore(categories: List<CategoryData>): Double {
        val totalTime = getTotalUsageTime(categories)
        if (totalTime == 0L) return 0.0

        val productiveTime = categories
            .filter { isProductiveCategory(it.categoryName) }
            .sumOf { it.totalTime }

        val entertainmentTime = categories
            .filter { isEntertainmentCategory(it.categoryName) }
            .sumOf { it.totalTime }

        // Calculate productivity score (0-100)
        val productiveRatio = productiveTime.toDouble() / totalTime
        val entertainmentRatio = entertainmentTime.toDouble() / totalTime

        return max(0.0, min(100.0, (productiveRatio * 70 + (1 - entertainmentRatio) * 30) * 100))
    }

    fun getUsageInsights(categories: List<CategoryData>): List<String> {
        val insights = mutableListOf<String>()
        val totalScreenTime = getTotalUsageTime(categories)

        if (totalScreenTime == 0L) {
            insights.add("No usage data available.")
            return insights
        }

        // Find most used category
        val mostUsedCategory = categories.maxByOrNull { it.totalTime }
        mostUsedCategory?.let { category ->
            val percentage = (category.totalTime.toDouble() / totalScreenTime) * 100
            insights.add("📱 You spent ${percentage.format(1)}% of your time on ${category.categoryName} apps")
        }

        // Screen time insights
        val hours = totalScreenTime / 3600000
        when {
            hours > 8 -> insights.add("⏰ Your screen time is quite high (${formatTime(totalScreenTime)}). Consider taking breaks.")
            hours > 4 -> insights.add("📊 Your screen time is moderate (${formatTime(totalScreenTime)}).")
            else -> insights.add("✅ You're maintaining healthy screen time habits (${formatTime(totalScreenTime)}).")
        }

        // Productivity insights
        val productivityScore = getProductivityScore(categories)
        when {
            productivityScore >= 70 -> insights.add("🎯 Great productivity score (${productivityScore.format(0)}%)!")
            productivityScore >= 40 -> insights.add("📈 Decent productivity balance (${productivityScore.format(0)}%).")
            else -> insights.add("📱 Consider balancing entertainment with productive apps (${productivityScore.format(0)}%).")
        }

        // Category-specific insights
        categories.forEach { category ->
            val percentage = (category.totalTime.toDouble() / totalScreenTime) * 100
            when {
                category.categoryName == "Social Media" && percentage > 30 -> {
                    insights.add("💭 High social media usage (${percentage.format(1)}%). Try setting app timers.")
                }
                category.categoryName == "Games" && percentage > 25 -> {
                    insights.add("🎮 Significant gaming time (${percentage.format(1)}%). Balance with other activities.")
                }
                category.categoryName == "Entertainment" && percentage > 35 -> {
                    insights.add("📺 Heavy entertainment consumption (${percentage.format(1)}%). Mix in educational content.")
                }
                category.categoryName == "Productivity" && percentage > 30 -> {
                    insights.add("💼 Excellent focus on productive activities (${percentage.format(1)}%)!")
                }
            }
        }

        return insights.take(5) // Limit to 5 insights
    }

    fun getHourlyBreakdown(categories: List<CategoryData>): Map<String, Map<Int, Long>> {
        // This would require more detailed session data
        // For now, return empty map as placeholder
        return emptyMap()
    }

    fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${milliseconds / 1000}s"
        }
    }

    private fun isProductiveCategory(categoryName: String): Boolean {
        return setOf(
            "Productivity", "Education", "Business",
            "News & Reading", "Finance"
        ).contains(categoryName)
    }

    private fun isEntertainmentCategory(categoryName: String): Boolean {
        return setOf(
            "Entertainment", "Games", "Social Media",
            "Music", "Sports", "Comedy", "Fashion", "Adult"
        ).contains(categoryName)
    }

    private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
}

data class CategoryStats(
    val categoryName: String,
    val totalTime: Long,
    val appCount: Int,
    val percentage: Double,
    val topApps: List<AppUsageData>,
    val averageSessionTime: Long,
    val color: String
)

data class AppCategoryStats(
    val appName: String,
    val packageName: String,
    val timeInCategory: Long,
    val categoryName: String,
    val percentageInCategory: Double,
    val launchCount: Int
)