package com.lohith.scrollsense.data

import androidx.room.*

@Dao
interface UserFeedbackDao {
    @Query("SELECT * FROM user_feedback WHERE packageName = :packageName")
    suspend fun getFeedbackForPackage(packageName: String): UserFeedback?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(feedback: UserFeedback)

    @Query("UPDATE user_feedback SET feedbackCount = feedbackCount + 1, lastUpdated = :timestamp WHERE packageName = :packageName")
    suspend fun incrementFeedbackCount(packageName: String, timestamp: Long)

    @Query("SELECT * FROM user_feedback ORDER BY feedbackCount DESC")
    suspend fun getAllFeedback(): List<UserFeedback>
}
