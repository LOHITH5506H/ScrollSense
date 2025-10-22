package com.lohith.scrollsense.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_segments")
data class ContentSegment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val contentType: String,     // e.g., "video", "text", "image", "shopping", "news"
    val startTimeMs: Long,
    val endTimeMs: Long          // when closed; if still open, set when next type/app starts
) {
    val durationMs: Long get() = (endTimeMs - startTimeMs).coerceAtLeast(0L)
}
