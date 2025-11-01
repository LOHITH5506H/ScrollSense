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

    // --- Added utilities for clear/prune ---
    @Query("DELETE FROM daily_summaries")
    suspend fun clearAllSummaries()

    @Query("DELETE FROM daily_category_analytics")
    suspend fun clearAllCategoryAnalytics()

    @Query("DELETE FROM daily_app_analytics")
    suspend fun clearAllAppAnalytics()

    @Query("DELETE FROM daily_summaries WHERE date < :minDate")
    suspend fun deleteSummariesOlderThan(minDate: String)

    @Query("DELETE FROM daily_category_analytics WHERE date < :minDate")
    suspend fun deleteCategoryAnalyticsOlderThan(minDate: String)

    @Query("DELETE FROM daily_app_analytics WHERE date < :minDate")
    suspend fun deleteAppAnalyticsOlderThan(minDate: String)
}
