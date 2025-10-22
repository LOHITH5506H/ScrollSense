package com.lohith.scrollsense.ui

import androidx.lifecycle.*
import com.lohith.scrollsense.data.AppDatabase
import kotlinx.coroutines.launch

data class AppTypeTotal(val packageName: String, val contentType: String, val totalMs: Long)

class ChartsViewModel(private val db: AppDatabase) : ViewModel() {

    private val _appTypeTotals = MutableLiveData<List<AppTypeTotal>>()
    val appTypeTotals: LiveData<List<AppTypeTotal>> = _appTypeTotals

    fun load(fromMs: Long, toMs: Long) {
        viewModelScope.launch {
            val rows = db.contentSegmentDao().getTotalsByAppAndType(fromMs, toMs)
            _appTypeTotals.postValue(rows.map { AppTypeTotal(it.packageName, it.contentType, it.totalMs) })
        }
    }
}

class ChartsVMFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChartsViewModel(db) as T
    }
}
