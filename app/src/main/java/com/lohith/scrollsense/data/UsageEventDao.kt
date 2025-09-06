package com.lohith.scrollsense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: UsageEvent): Long // return row id

    @Query("SELECT * FROM usage_events ORDER BY startTime DESC")
    fun getAllEvents(): Flow<List<UsageEvent>>

    @Query("SELECT * FROM usage_events WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getEventsSince(startTime: Long): Flow<List<UsageEvent>>

    @Query("SELECT appLabel, SUM(durationMs) as totalDuration FROM usage_events WHERE startTime >= :startTime GROUP BY appLabel ORDER BY totalDuration DESC")
    fun getAppUsageStats(startTime: Long): Flow<List<AppUsageStat>>

    @Query("SELECT category, COUNT(*) as count FROM usage_events WHERE startTime >= :startTime GROUP BY category ORDER BY count DESC")
    fun getCategoryStats(startTime: Long): Flow<List<CategoryStat>>

    @Query("SELECT category AS category, SUM(durationMs) AS totalDuration FROM usage_events WHERE startTime >= :startTime GROUP BY category ORDER BY totalDuration DESC")
    suspend fun getCategoryDurationsSince(startTime: Long): List<CategoryDuration>

    // New: fetch all events overlapping a [start, end) window
    @Query("SELECT * FROM usage_events WHERE endTime > :start AND startTime < :end")
    suspend fun getEventsOverlapping(start: Long, end: Long): List<UsageEvent>

    @Query("UPDATE usage_events SET durationMs = :duration WHERE id = (SELECT MAX(id) FROM usage_events)")
    suspend fun updateLastEventDuration(duration: Long)

    @Query("UPDATE usage_events SET endTime = :endTime, durationMs = :duration WHERE id = :id")
    suspend fun updateEventEnd(id: Long, endTime: Long, duration: Long)

    @Query("DELETE FROM usage_events")
    suspend fun clearAll()
}

data class AppUsageStat(
    val appLabel: String,
    val totalDuration: Long
)

data class CategoryStat(
    val category: String,
    val count: Long
)

data class CategoryDuration(
    val category: String,
    val totalDuration: Long
)
