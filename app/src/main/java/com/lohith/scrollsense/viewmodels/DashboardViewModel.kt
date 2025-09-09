package com.lohith.scrollsense.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lohith.scrollsense.data.UsageRepository
import com.lohith.scrollsense.data.models.TotalByType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _usageByCategory = MutableLiveData<List<TotalByType>>()
    val usageByCategory: LiveData<List<TotalByType>> = _usageByCategory

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadUsageDataForToday() {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val endTime = System.currentTimeMillis()
                val startTime = endTime - TimeUnit.DAYS.toMillis(1)

                val data = withContext(Dispatchers.IO) {
                    usageRepository.getTotalsForPeriod(startTime, endTime)
                } ?: emptyList()

                _usageByCategory.postValue(data)
            } catch (e: Exception) {
                _usageByCategory.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}