package com.lohith.scrollsense.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_segments")
data class ContentSegment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val contentType: String,
    val contentTitle: String, // Ensure this is a non-nullable String
    val startTimeMs: Long,
    val endTimeMs: Long,
    val duration: Long
)