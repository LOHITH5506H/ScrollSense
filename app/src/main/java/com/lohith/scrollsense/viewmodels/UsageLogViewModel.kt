package com.lohith.scrollsense.viewmodels

import androidx.lifecycle.ViewModel
import com.lohith.scrollsense.data.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UsageLogViewModel @Inject constructor(
    private val usageRepository: UsageRepository
) : ViewModel() {
    // This LiveData will hold the complete list of usage events
    // for our Compose screen to observe.
    val allUsageSegments = usageRepository.getAllSegments()
}