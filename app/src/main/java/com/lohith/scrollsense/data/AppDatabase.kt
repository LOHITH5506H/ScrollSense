package com.lohith.scrollsense.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UsageEvent::class, ContentSegment::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageEventDao(): UsageEventDao
    abstract fun contentSegmentDao(): ContentSegmentDao

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

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }

        // Legacy alias for existing code paths
        fun get(context: Context): AppDatabase = getDatabase(context)
    }
}
