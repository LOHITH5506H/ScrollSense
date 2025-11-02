package com.lohith.scrollsense.data

import androidx.room.*

@Dao
interface DailySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailySummary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryAnalytics(analytics: List<DailyCategoryAnalytics>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppAnalytics(analytics: List<DailyAppAnalytics>)

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    suspend fun getDailySummary(date: String): DailySummary?

    @Query("SELECT * FROM daily_summaries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getDailySummaries(startDate: String, endDate: String): List<DailySummary>

    @Query("SELECT * FROM daily_category_analytics WHERE date = :date ORDER BY totalTimeMs DESC")
    suspend fun getDailyCategoryAnalytics(date: String): List<DailyCategoryAnalytics>

    @Query("SELECT * FROM daily_category_analytics WHERE date >= :startDate AND date <= :endDate")
    suspend fun getCategoryAnalyticsRange(startDate: String, endDate: String): List<DailyCategoryAnalytics>

    @Query("SELECT * FROM daily_app_analytics WHERE date = :date ORDER BY totalTimeMs DESC")
    suspend fun getDailyAppAnalytics(date: String): List<DailyAppAnalytics>

    @Query("SELECT * FROM daily_app_analytics WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAppAnalyticsRange(startDate: String, endDate: String): List<DailyAppAnalytics>

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC LIMIT 7")
    suspend fun getLast7Days(): List<DailySummary>

    // --- ADDED TO FIX VIEWMODEL ---

    // --- Clear All Functions ---

    @Query("DELETE FROM daily_summaries")
    suspend fun clearAllSummaries()

    @Query("DELETE FROM daily_category_analytics")
    suspend fun clearAllCategoryAnalytics()

    @Query("DELETE FROM daily_app_analytics")
    suspend fun clearAllAppAnalytics()

    // --- Pruning (Delete Older Than) Functions ---

    /**
     * Deletes summaries older than the given date string.
     * @param yyyyMmDd The date string in "YYYY-MM-DD" format.
     */
    @Query("DELETE FROM daily_summaries WHERE date < :yyyyMmDd")
    suspend fun deleteSummariesOlderThan(yyyyMmDd: String)

    /**
     * Deletes category analytics older than the given date string.
     * @param yyyyMmDd The date string in "YYYY-MM-DD" format.
     */
    @Query("DELETE FROM daily_category_analytics WHERE date < :yyyyMmDd")
    suspend fun deleteCategoryAnalyticsOlderThan(yyyyMmDd: String)

    /**
     * Deletes app analytics older than the given date string.
     * @param yyyyMmDd The date string in "YYYY-MM-DD" format.
     */
    @Query("DELETE FROM daily_app_analytics WHERE date < :yyyyMmDd")
    suspend fun deleteAppAnalyticsOlderThan(yyyyMmDd: String)
}
