package com.lohith.scrollsense.data

import androidx.room.Entity

@Entity(
    tableName = "daily_app_analytics",
    primaryKeys = ["date", "packageName"]
)
data class DailyAppAnalytics(
    val date: String,
    val packageName: String,
    val appName: String,
    val category: String,
    val totalTimeMs: Long,
    val sessionsCount: Int,
    val percentageOfDay: Float
)
