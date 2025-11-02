package com.lohith.scrollsense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContentSegmentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(segment: ContentSegment): Long

    @Query("UPDATE content_segments SET endTimeMs = :endTimeMs WHERE id = :id")
    suspend fun closeSegment(id: Long, endTimeMs: Long)

    // Returns totals per app and type within a window (overlapping)
    @Query("""
        SELECT packageName, contentType, SUM(
          CASE 
           WHEN endTimeMs < :fromMs OR startTimeMs > :toMs THEN 0
           ELSE (MIN(endTimeMs, :toMs) - MAX(startTimeMs, :fromMs))
          END
        ) AS totalMs
        FROM content_segments
        GROUP BY packageName, contentType
        HAVING totalMs > 0
        ORDER BY packageName ASC, totalMs DESC
    """)
    suspend fun getTotalsByAppAndType(fromMs: Long, toMs: Long): List<TotalByAppAndType>

    @Query("""
        SELECT contentType, SUM(
          CASE 
           WHEN endTimeMs < :fromMs OR startTimeMs > :toMs THEN 0
           ELSE (MIN(endTimeMs, :toMs) - MAX(startTimeMs, :fromMs))
          END
        ) AS totalMs
        FROM content_segments
        WHERE packageName = :pkg
        GROUP BY contentType
        HAVING totalMs > 0
        ORDER BY totalMs DESC
    """)
    suspend fun getTotalsForApp(pkg: String, fromMs: Long, toMs: Long): List<TotalByType>

    // --- ADDED TO FIX VIEWMODEL ---
    /**
     * Deletes all entries from the content_segments table.
     */
    @Query("DELETE FROM content_segments")
    suspend fun clearAll()

    /**
     * Deletes all segments older than the given timestamp.
     * @param cutoffTime The epoch timestamp (in milliseconds). Segments before this time will be deleted.
     */
    @Query("DELETE FROM content_segments WHERE startTimeMs < :cutoffTime") // Assuming timestamp column is 'startTimeMs'
    suspend fun deleteOlderThan(cutoffTime: Long)
}

data class TotalByAppAndType(
    val packageName: String,
    val contentType: String,
    val totalMs: Long
)

data class TotalByType(
    val contentType: String,
    val totalMs: Long
)
