package com.lohith.scrollsense.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UsageEvent::class],
    version = 2, // Increment version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageEventDao(): UsageEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "usage_database"
                )
                    .addMigrations(MIGRATION_1_2) // Add migration
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE usage_events ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE usage_events ADD COLUMN language TEXT NOT NULL DEFAULT 'en'")
                database.execSQL("ALTER TABLE usage_events ADD COLUMN confidence REAL NOT NULL DEFAULT 1.0")
            }
        }
    }
}

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromBoolean(value: Boolean?): Int? = value?.let { if (it) 1 else 0 }

    @TypeConverter
    @JvmStatic
    fun toBoolean(value: Int?): Boolean? = value?.let { it == 1 }
}
