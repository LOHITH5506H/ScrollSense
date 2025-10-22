package com.lohith.scrollsense.data

import androidx.room.Entity

@Entity(
    tableName = "daily_category_analytics",
    primaryKeys = ["date", "category"]
)
data class DailyCategoryAnalytics(
    val date: String,
    val category: String,
    val totalTimeMs: Long,
    val sessionsCount: Int,
    val percentageOfDay: Float
)
