package com.lohith.scrollsense.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val totalScreenTimeMs: Long,
    val topCategory: String,
    val topApp: String,
    val sessionsCount: Int,
    val averageSessionDurationMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)
