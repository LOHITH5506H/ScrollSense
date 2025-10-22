@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lohith.scrollsense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.UsageEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// Data class to hold aggregated category data
data class CategoryUsage(
    val categoryName: String,
    val totalDuration: Long
)

// Data class to hold aggregated app data
data class AppUsage(
    val appName: String,
    val totalDuration: Long
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val usageEventDao = AppDatabase.getDatabase(application).usageEventDao()

    // Holds the currently selected date range ("today", "week", etc.)
    private val _dateRange = MutableStateFlow("today")
    val dateRange: StateFlow<String> = _dateRange.asStateFlow()

    // --- DATA FLOWS ---

    // Flow of all raw UsageEvents for the selected date range
    val usageEvents: StateFlow<List<UsageEvent>> = _dateRange.flatMapLatest { range ->
        val startTime = getStartTimeForRange(range)
        usageEventDao.getEventsSince(startTime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Flow of CATEGORY usage, aggregated from UsageEvents
    val categoryUsage: StateFlow<List<CategoryUsage>> = usageEvents.map { events ->
        events.groupBy { it.category }
            .map { (category, eventList) ->
                CategoryUsage(
                    categoryName = category,
                    totalDuration = eventList.sumOf { it.durationMs }
                )
            }
            .sortedByDescending { it.totalDuration }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Flow of APP usage, aggregated from UsageEvents
    val appUsage: StateFlow<List<AppUsage>> = usageEvents.map { events ->
        events.groupBy { it.appLabel }
            .map { (appName, eventList) ->
                AppUsage(
                    appName = appName,
                    totalDuration = eventList.sumOf { it.durationMs }
                )
            }
            .sortedByDescending { it.totalDuration }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- ACTIONS ---

    fun updateDateRange(newRange: String) {
        _dateRange.value = newRange
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            usageEventDao.clearAll()
        }
    }

    private fun getStartTimeForRange(range: String): Long {
        val calendar = Calendar.getInstance()
        return when (range) {
            "week" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            "month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.timeInMillis
            }
            "today" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            else -> 0L
        }
    }
}

