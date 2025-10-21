package com.lohith.scrollsense.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_feedback")
data class UserFeedback(
    @PrimaryKey
    val packageName: String,
    val category: String,
    val confidence: Float,
    val feedbackCount: Int,
    val lastUpdated: Long
)
