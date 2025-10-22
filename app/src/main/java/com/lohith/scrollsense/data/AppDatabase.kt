package com.lohith.scrollsense.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UsageEvent::class, ContentSegment::class, UserFeedback::class, DailySummary::class, DailyCategoryAnalytics::class, DailyAppAnalytics::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageEventDao(): UsageEventDao
    abstract fun contentSegmentDao(): ContentSegmentDao
    abstract fun userFeedbackDao(): UserFeedbackDao
    abstract fun dailySummaryDao(): DailySummaryDao

    companion object {
        private const val DB_NAME = "scrollsense.db"

        // Migration 1 -> 2: add new columns to usage_events
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE usage_events ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) { /* column may already exist */ }
                try {
                    db.execSQL("ALTER TABLE usage_events ADD COLUMN language TEXT NOT NULL DEFAULT 'en'")
                } catch (_: Exception) { }
                try {
                    db.execSQL("ALTER TABLE usage_events ADD COLUMN confidence REAL NOT NULL DEFAULT 1.0")
                } catch (_: Exception) { }
            }
        }

        // Migration 2 -> 3: create content_segments table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS content_segments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        contentType TEXT NOT NULL,
                        startTimeMs INTEGER NOT NULL,
                        endTimeMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Migration 3 -> 4: add user feedback and daily analytics tables
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create user feedback table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_feedback (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        category TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        feedbackCount INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create daily summaries table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_summaries (
                        date TEXT PRIMARY KEY NOT NULL,
                        totalScreenTimeMs INTEGER NOT NULL,
                        topCategory TEXT NOT NULL,
                        topApp TEXT NOT NULL,
                        sessionsCount INTEGER NOT NULL,
                        averageSessionDurationMs INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create daily category analytics table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_category_analytics (
                        date TEXT NOT NULL,
                        category TEXT NOT NULL,
                        totalTimeMs INTEGER NOT NULL,
                        sessionsCount INTEGER NOT NULL,
                        percentageOfDay REAL NOT NULL,
                        PRIMARY KEY (date, category)
                    )
                """.trimIndent())

                // Create daily app analytics table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_app_analytics (
                        date TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        category TEXT NOT NULL,
                        totalTimeMs INTEGER NOT NULL,
                        sessionsCount INTEGER NOT NULL,
                        percentageOfDay REAL NOT NULL,
                        PRIMARY KEY (date, packageName)
                    )
                """.trimIndent())
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }

        // Legacy alias for existing code paths
        fun get(context: Context): AppDatabase = getDatabase(context)
    }
}
