package com.lohith.scrollsense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsageEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: UsageEvent)

    @Query("SELECT * FROM usage_events ORDER BY startTime DESC")
    suspend fun getAllEvents(): List<UsageEvent>

    @Query("DELETE FROM usage_events")
    suspend fun clearAll()
}
