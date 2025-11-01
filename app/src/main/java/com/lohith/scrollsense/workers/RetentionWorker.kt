package com.lohith.scrollsense.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.util.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class RetentionWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val db = AppDatabase.getDatabase(ctx)
        val prefs = PreferencesManager.get(ctx)

        val days = prefs.getRetentionDays()
        val cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())

        // Prune time-based tables
        db.usageEventDao().deleteOlderThan(cutoffMillis)
        db.contentSegmentDao().deleteOlderThan(cutoffMillis)

        // Prune daily analytics with date (yyyy-MM-dd)
        val cutoffDate = toDateString(cutoffMillis)
        val dailyDao = db.dailySummaryDao()
        dailyDao.deleteSummariesOlderThan(cutoffDate)
        dailyDao.deleteCategoryAnalyticsOlderThan(cutoffDate)
        dailyDao.deleteAppAnalyticsOlderThan(cutoffDate)

        return Result.success()
    }

    private fun toDateString(ms: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(ms)
    }

    companion object {
        private const val UNIQUE_NAME = "retention_pruner"

        fun schedule(context: Context) {
            val work = PeriodicWorkRequestBuilder<RetentionWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, work)
        }

        fun runNow(context: Context) {
            // Optional: could enqueue a OneTimeWork, but retention will run on next cycle.
        }
    }
}

