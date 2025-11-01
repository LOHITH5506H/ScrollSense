@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lohith.scrollsense.viewmodel

import android.app.Application
import android.os.Environment
import android.util.Log
import android.widget.Toast
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
import java.io.File
import java.util.Calendar

// ... (Your existing data classes: CategoryUsage, AppUsage) ...
data class CategoryUsage(
    val categoryName: String,
    val totalDuration: Long
)
data class AppUsage(
    val appName: String,
    val totalDuration: Long
)


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val usageEventDao = db.usageEventDao()
    private val contentSegmentDao = db.contentSegmentDao()
    private val dailySummaryDao = db.dailySummaryDao()

    private val _dateRange = MutableStateFlow("today")
    val dateRange: StateFlow<String> = _dateRange.asStateFlow()

    private val _selectedAppPackage = MutableStateFlow<String?>(null)
    val selectedAppPackage = _selectedAppPackage.asStateFlow()

    private val appNameBlocklist = setOf(
        "Gboard", "ScrollSense", "Pixel Launcher", "Launcher", "System UI"
    )

    val usageEvents: StateFlow<List<UsageEvent>> = _dateRange.flatMapLatest { range ->
        val startTime = getStartTimeForRange(range)
        usageEventDao.getEventsSince(startTime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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

    val appUsage: StateFlow<List<AppUsage>> = usageEvents.map { events ->
        events.filter { it.appLabel !in appNameBlocklist }
            .groupBy { it.appLabel }
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

    fun onAppClicked(appName: String?) {
        _selectedAppPackage.value = appName
    }

    fun updateDateRange(newRange: String) {
        _dateRange.value = newRange
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            usageEventDao.clearAll()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            usageEventDao.clearAll()
            contentSegmentDao.clearAll()
            dailySummaryDao.clearAllSummaries()
            dailySummaryDao.clearAllCategoryAnalytics()
            dailySummaryDao.clearAllAppAnalytics()
        }
    }

    fun pruneOlderThan(days: Int) {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - days.toLong() * 24L * 60L * 60L * 1000L
            usageEventDao.deleteOlderThan(cutoff)
            contentSegmentDao.deleteOlderThan(cutoff)
            val cal = Calendar.getInstance().apply { timeInMillis = cutoff }
            val yyyyMmDd = String.format(
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dailySummaryDao.deleteSummariesOlderThan(yyyyMmDd)
            dailySummaryDao.deleteCategoryAnalyticsOlderThan(yyyyMmDd)
            dailySummaryDao.deleteAppAnalyticsOlderThan(yyyyMmDd)
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
            "today" -> startOfDayMillis()
            else -> 0L
        }
    }

    private fun startOfDayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // --- NEW: BACKUP/RESTORE FUNCTIONS ---

    private val dbName = "app_database.db"

    fun backupDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // CRITICAL: Close the database to flush WAL files
                db.close()

                val context = getApplication<Application>()
                val dbPath = context.getDatabasePath(dbName)
                val shmPath = File(dbPath.parent, "$dbName-shm")
                val walPath = File(dbPath.parent, "$dbName-wal")

                val backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                val backupDbFile = File(backupDir, "scrollsense_backup.db")
                val backupShmFile = File(backupDir, "scrollsense_backup.db-shm")
                val backupWalFile = File(backupDir, "scrollsense_backup.db-wal")

                // Copy all three files
                dbPath.copyTo(backupDbFile, overwrite = true)
                if (shmPath.exists()) shmPath.copyTo(backupShmFile, overwrite = true)
                if (walPath.exists()) walPath.copyTo(backupWalFile, overwrite = true)

                Log.d("Backup", "Backup successful to ${backupDbFile.absolutePath}")
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Backup Successful!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Backup", "Backup Failed", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Backup Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // Re-open the database
                AppDatabase.getDatabase(getApplication())
            }
        }
    }

    fun restoreDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // CRITICAL: Close the database
                db.close()

                val context = getApplication<Application>()
                val dbPath = context.getDatabasePath(dbName)
                val shmPath = File(dbPath.parent, "$dbName-shm")
                val walPath = File(dbPath.parent, "$dbName-wal")

                val backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val backupDbFile = File(backupDir, "scrollsense_backup.db")
                val backupShmFile = File(backupDir, "scrollsense_backup.db-shm")
                val backupWalFile = File(backupDir, "scrollsense_backup.db-wal")

                if (!backupDbFile.exists()) {
                    Log.e("Restore", "No backup file found")
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "No backup file found", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Copy all three files
                backupDbFile.copyTo(dbPath, overwrite = true)
                if (backupShmFile.exists()) backupShmFile.copyTo(shmPath, overwrite = true)
                if (backupWalFile.exists()) backupWalFile.copyTo(walPath, overwrite = true)

                Log.d("Restore", "Restore successful from ${backupDbFile.absolutePath}")
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Restore Successful! Restart app.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("Restore", "Restore Failed", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Restore Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // Re-open the database
                AppDatabase.getDatabase(getApplication())
            }
        }
    }
}