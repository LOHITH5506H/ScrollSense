package com.lohith.scrollsense.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.UsageEvent
import kotlinx.coroutines.launch

// ---------- ViewModel ----------
class UsageLogViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).usageEventDao()
    var events by mutableStateOf(listOf<UsageEvent>())
        private set

    fun loadEvents() {
        viewModelScope.launch {
            events = dao.getAllEvents().sortedByDescending { it.startTime }
        }
    }
}

// ---------- Composable Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageLogScreen(vm: UsageLogViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        vm.loadEvents()
    }

    Scaffold(
        topBar = {
            TopAppBar(                      // ✅ Use TopAppBar instead of SmallTopAppBar
                title = { Text("ScrollSense Logs") }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            items(vm.events) { event ->
                EventItem(event)
            }
        }
    }
}

@Composable
fun EventItem(event: UsageEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(text = "App: ${event.appLabel}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Screen: ${event.screenTitle}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "Category: ${event.category}", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "Duration: ${event.durationMs / 1000}s", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
    }
}
