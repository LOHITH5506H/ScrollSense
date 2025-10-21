package com.lohith.scrollsense.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.lohith.scrollsense.analytics.AnalyticsProcessor
import com.lohith.scrollsense.data.HybridUsageTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*

/**
 * Background service that runs daily analytics processing and screen time synchronization
 */
class AnalyticsBackgroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var analyticsProcessor: AnalyticsProcessor
    private lateinit var hybridUsageTracker: HybridUsageTracker

    companion object {
        private const val TAG = "AnalyticsService"
    }

    override fun onCreate() {
        super.onCreate()
        analyticsProcessor = AnalyticsProcessor(this)
        hybridUsageTracker = HybridUsageTracker(this)
        Log.d(TAG, "Analytics Background Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Analytics Background Service Started")

        serviceScope.launch {
            try {
                // Process analytics for yesterday and today
                val calendar = Calendar.getInstance()
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                // Process yesterday's complete data
                analyticsProcessor.processDayAnalytics(yesterday)

                // Update today's data (will be processed again tomorrow)
                analyticsProcessor.processDayAnalytics(today)

                Log.d(TAG, "Successfully processed analytics for $yesterday and $today")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing daily analytics", e)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Analytics Background Service Destroyed")
    }
}
