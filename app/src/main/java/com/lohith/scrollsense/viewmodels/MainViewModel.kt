package com.lohith.scrollsense.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lohith.scrollsense.data.models.AppUsageData
import com.lohith.scrollsense.data.models.CategoryData

class MainViewModel : ViewModel() {

    private val _appUsageData = MutableLiveData<List<AppUsageData>>()
    val appUsageData: LiveData<List<AppUsageData>> = _appUsageData

    private val _categoryData = MutableLiveData<List<CategoryData>>()
    val categoryData: LiveData<List<CategoryData>> = _categoryData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _selectedTimeRange = MutableLiveData<TimeRange>()
    val selectedTimeRange: LiveData<TimeRange> = _selectedTimeRange

    init {
        _isLoading.value = false
        _selectedTimeRange.value = TimeRange.TODAY
        _appUsageData.value = emptyList()
        _categoryData.value = emptyList()
    }

    fun updateData(apps: List<AppUsageData>, categories: List<CategoryData>) {
        _appUsageData.value = apps
        _categoryData.value = categories
    }

    fun updateAppUsageData(apps: List<AppUsageData>) {
        _appUsageData.value = apps
    }

    fun updateCategoryData(categories: List<CategoryData>) {
        _categoryData.value = categories
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setError(error: String) {
        _errorMessage.value = error
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setTimeRange(timeRange: TimeRange) {
        _selectedTimeRange.value = timeRange
    }

    fun clearData() {
        _appUsageData.value = emptyList()
        _categoryData.value = emptyList()
    }

    fun getTotalScreenTime(): Long {
        return _appUsageData.value?.sumOf { it.totalTimeInForeground } ?: 0L
    }

    fun getMostUsedApp(): AppUsageData? {
        return _appUsageData.value?.maxByOrNull { it.totalTimeInForeground }
    }

    fun getMostUsedCategory(): CategoryData? {
        return _categoryData.value?.maxByOrNull { it.totalTime }
    }

    fun getTopApps(limit: Int = 10): List<AppUsageData> {
        return _appUsageData.value
            ?.sortedByDescending { it.totalTimeInForeground }
            ?.take(limit) ?: emptyList()
    }

    fun getTopCategories(limit: Int = 8): List<CategoryData> {
        return _categoryData.value
            ?.sortedByDescending { it.totalTime }
            ?.take(limit) ?: emptyList()
    }

    fun getAppsByCategory(categoryName: String): List<AppUsageData> {
        return _categoryData.value
            ?.find { it.categoryName == categoryName }
            ?.apps ?: emptyList()
    }

    fun hasData(): Boolean {
        return !_appUsageData.value.isNullOrEmpty()
    }
}

enum class TimeRange(val displayName: String, val days: Int) {
    TODAY("Today", 1),
    WEEK("This Week", 7),
    MONTH("This Month", 30)
}