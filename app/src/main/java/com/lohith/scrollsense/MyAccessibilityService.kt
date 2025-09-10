package com.lohith.scrollsense

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.UsageEvent
import com.lohith.scrollsense.util.CategoryClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "ScrollSenseSvc"
private const val DEBOUNCE_DELAY_MS = 750L // Wait this long after an event before processing

class MyAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val usageEventDao by lazy { db.usageEventDao() }

    // --- State variables to track the current session ---
    private var currentSessionId: Long? = null
    private var currentPackageName: String? = null
    private var currentCategory: String? = null
    private var lastEventTime: Long = 0

    // --- Debounce handler to avoid processing too many events ---
    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    private val ignoredPackages = setOf("com.android.systemui", "com.mi.android.globallauncher")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString()
        if (packageName.isNullOrBlank() || ignoredPackages.contains(packageName) || packageName == applicationContext.packageName) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // Cancel any pending runnable
            debounceRunnable?.let { debounceHandler.removeCallbacks(it) }

            // Create a new runnable to process the event after a delay
            debounceRunnable = Runnable {
                processEvent(event)
            }
            debounceHandler.postDelayed(debounceRunnable!!, DEBOUNCE_DELAY_MS)
        }
    }

    private fun processEvent(event: AccessibilityEvent) {
        val packageName = event.packageName.toString()
        val eventTime = System.currentTimeMillis()

        val source = event.source
        if (source == null) {
            endCurrentSession(eventTime)
            return
        }

        val screenTextBuilder = StringBuilder()
        collectTextFromNodes(source, screenTextBuilder)
        val screenText = screenTextBuilder.toString().trim()
        source.recycle()

        if (screenText.isBlank()) {
            return
        }

        serviceScope.launch {
            val newCategory = CategoryClassifier.classifyContent(screenText, packageName)

            // If the app or category hasn't changed, do nothing.
            if (packageName == currentPackageName && newCategory == currentCategory) {
                return@launch
            }

            // A significant change occurred. End the old session and start a new one.
            endCurrentSession(eventTime)
            startNewSession(packageName, screenText, newCategory, eventTime)
        }
    }

    private fun startNewSession(packageName: String, screenTitle: String, category: String, startTime: Long) {
        serviceScope.launch {
            val appLabel = getAppName(packageName)
            val newEvent = UsageEvent(
                startTime = startTime,
                endTime = startTime,
                durationMs = 0,
                packageName = packageName,
                appLabel = appLabel,
                screenTitle = screenTitle,
                category = category
            )
            val id = usageEventDao.insert(newEvent)

            // Update current session state
            currentSessionId = id
            currentPackageName = packageName
            currentCategory = category
            lastEventTime = startTime

            Log.d(TAG, "Start session id=$id, pkg=$appLabel, category=$category")
        }
    }

    private fun endCurrentSession(endTime: Long) {
        val capturedSessionId = currentSessionId ?: return

        val duration = endTime - lastEventTime
        if (duration < 500) { // Discard sessions less than 0.5s
            serviceScope.launch {
                usageEventDao.deleteById(capturedSessionId)
                Log.d(TAG, "Discarded short session id=$capturedSessionId, duration=${duration}ms")
            }
        } else {
            serviceScope.launch {
                usageEventDao.updateSessionEndTime(capturedSessionId, endTime, duration)
                Log.d(TAG, "End session id=$capturedSessionId, duration=${duration}ms")
            }
        }

        // Clear current session state
        currentSessionId = null
        currentPackageName = null
        currentCategory = null
    }

    private fun getAppName(packageName: String): String {
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun collectTextFromNodes(node: android.view.accessibility.AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null || !node.isVisibleToUser) return
        if (node.text != null && node.text.isNotBlank()) {
            builder.append(node.text).append(" ")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            collectTextFromNodes(child, builder)
            child?.recycle()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted.")
        serviceJob.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        endCurrentSession(System.currentTimeMillis())
        serviceJob.cancel()
        Log.d(TAG, "Accessibility service destroyed.")
    }
}