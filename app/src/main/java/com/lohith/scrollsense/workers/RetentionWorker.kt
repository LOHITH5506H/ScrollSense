package com.lohith.scrollsense.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.util.PreferencesManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * A periodic worker that prunes old data from the database based on user's retention settings.
 * This ensures the database doesn't grow indefinitely.
 */
class RetentionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORK_NAME = "RetentionWorker"
        private const val TAG = "RetentionWorker"

        /**
         * Schedules the periodic retention worker.
         * It replaces any existing worker to ensure the repeat interval matches the setting.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(true)
                .build()

            val request = PeriodicWorkRequestBuilder<RetentionWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Periodic retention worker scheduled.")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "RetentionWorker starting...")
        return try {
            val prefs = PreferencesManager.get(context)
            val retentionDays = prefs.getRetentionDays()

            if (retentionDays <= 0) {
                Log.d(TAG, "Retention policy is 'Keep Forever'. Skipping prune.")
                return Result.success()
            }

            val cutoff = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -retentionDays)
            }.timeInMillis

            val db = AppDatabase.getDatabase(context)
            val eventDao = db.usageEventDao()
            val segmentDao = db.contentSegmentDao()
            val summaryDao = db.dailySummaryDao()

            val eventsDeleted = eventDao.deleteOlderThan(cutoff)
            val segmentsDeleted = segmentDao.deleteOlderThan(cutoff)
            Log.d(TAG, "Pruned $eventsDeleted events and $segmentsDeleted segments older than $retentionDays days.")

            // Prune analytics data
            val cal = Calendar.getInstance().apply { timeInMillis = cutoff }
            val yyyyMmDd = String.format(
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            val summariesDeleted = summaryDao.deleteSummariesOlderThan(yyyyMmDd)
            val appAnalyticsDeleted = summaryDao.deleteAppAnalyticsOlderThan(yyyyMmDd)
            val catAnalyticsDeleted = summaryDao.deleteCategoryAnalyticsOlderThan(yyyyMmDd)
            Log.d(TAG, "Pruned $summariesDeleted summaries, $appAnalyticsDeleted app analytics, $catAnalyticsDeleted category analytics.")

            Log.d(TAG, "RetentionWorker finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "RetentionWorker failed", e)
            Result.retry()
        }
    }
}