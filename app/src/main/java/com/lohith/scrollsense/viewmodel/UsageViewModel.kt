@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.lohith.scrollsense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.UsageEvent
import com.lohith.scrollsense.util.ExportUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val usageEventDao = database.usageEventDao()

    private val _dateRange = MutableStateFlow("today")
    val dateRange = _dateRange.asStateFlow()

    private val _selectedApp = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow<String?>(null)

    val usageEvents: StateFlow<List<UsageEvent>> = _dateRange.flatMapLatest { range ->
        val startTime = getStartTimeForRange(range)
        usageEventDao.getEventsSince(startTime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val appUsageStats: StateFlow<Map<String, Long>> = _dateRange.flatMapLatest { range ->
        val startTime = getStartTimeForRange(range)
        usageEventDao.getAppUsageStats(startTime)
    }.map { stats ->
        stats.associate { it.appLabel to it.totalDuration }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val categoryStats: StateFlow<Map<String, Long>> = _dateRange.flatMapLatest { range ->
        val startTime = getStartTimeForRange(range)
        usageEventDao.getCategoryStats(startTime)
    }.map { stats ->
        stats.associate { it.category to it.count }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun updateDateRange(range: String) {
        _dateRange.value = range
    }

    fun selectApp(app: String) {
        _selectedApp.value = app
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            usageEventDao.clearAll()
        }
    }

    fun exportData() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val events = usageEvents.value
            ExportUtil.shareUsageEvents(ctx, events)
        }
    }

    private fun getStartTimeForRange(range: String): Long {
        val calendar = Calendar.getInstance()
        return when (range) {
            "today" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            "week" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.timeInMillis
            }
            "month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.timeInMillis
            }
            "all" -> 0L
            else -> System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        }
    }
}
