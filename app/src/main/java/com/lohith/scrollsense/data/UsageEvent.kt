package com.lohith.scrollsense.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_events")
data class UsageEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appLabel: String,
    val screenTitle: String,
    val category: String,  // ✅ Added category field
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long
)
