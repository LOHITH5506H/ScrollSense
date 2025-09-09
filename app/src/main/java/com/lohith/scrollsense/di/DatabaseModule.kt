package com.lohith.scrollsense.di

import android.content.Context
import androidx.room.Room
import com.lohith.scrollsense.data.database.AppDatabase
import com.lohith.scrollsense.data.database.ContentSegmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "scrollsense_database"
        ).build()
    }

    @Provides
    fun provideContentSegmentDao(appDatabase: AppDatabase): ContentSegmentDao {
        return appDatabase.contentSegmentDao()
    }

    // The function that provided UsageEventDao has been removed.
}