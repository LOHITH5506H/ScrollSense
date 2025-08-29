@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.lohith.scrollsense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lohith.scrollsense.data.AppDatabase
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * ViewModel focused on providing chart-friendly data sets (maps of label->value)
 * decoupled from the broader UsageViewModel concerns.
 */
class ChartViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).usageEventDao()

    private val _range = MutableStateFlow("today")
    val range: StateFlow<String> = _range.asStateFlow()

    val appUsage: StateFlow<Map<String, Long>> = _range
        .flatMapLatest { r ->
            val start = rangeStart(r)
            dao.getAppUsageStats(start)
        }
        .map { list -> list.associate { it.appLabel to it.totalDuration } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categoryUsage: StateFlow<Map<String, Long>> = _range
        .flatMapLatest { r ->
            val start = rangeStart(r)
            dao.getCategoryStats(start)
        }
        .map { list -> list.associate { it.category to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setRange(r: String) { _range.value = r }

    private fun rangeStart(range: String): Long {
        val cal = Calendar.getInstance()
        return when (range) {
            "today" -> cal.apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            "week" -> cal.apply { add(Calendar.WEEK_OF_YEAR, -1) }.timeInMillis
            "month" -> cal.apply { add(Calendar.MONTH, -1) }.timeInMillis
            else -> 0L
        }
    }
}
