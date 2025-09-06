@file:OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)

// Enhanced MyAccessibilityService.kt
// Location: app/src/main/java/com/lohith/scrollsense/MyAccessibilityService.kt

package com.lohith.scrollsense

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.launch
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.UsageEvent
import com.lohith.scrollsense.util.CategoryClassifier
import com.lohith.scrollsense.util.PackageNameHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import android.util.Log

class MyAccessibilityService : AccessibilityService() {
    companion object { private const val TAG = "ScrollSenseSvc" }

    private lateinit var database: AppDatabase
    private var currentPackage: String? = null
    private var sessionStart: Long = 0
    private var currentEventId: Long? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastExtendUpdate: Long = 0L
    // Track current content category to create per-category segments
    private var currentCategory: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        database = AppDatabase.getDatabase(this)
        Log.d(TAG, "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == packageName()) return // ignore own app
        val now = System.currentTimeMillis()
        Log.d(TAG, "Event type=${event.eventType} pkg=$packageName source=${event.className}")

        val screenTitle = extractMeaningfulTitle(event, packageName)
        if (isSystemNoise(screenTitle)) return

        // If we are in the same package, check if category changed; if so, end/start a new segment.
        if (packageName == currentPackage) {
            val newCategory = CategoryClassifier.classifyContent(screenTitle, packageName)

            if (newCategory != currentCategory) {
                // Close previous segment and start a new one with the new category
                endCurrentSession(now)
                startNewSession(packageName, screenTitle, newCategory, now)
                return
            }

            // Same category: extend periodically to keep charts fresh
            val elapsed = now - sessionStart
            if (elapsed >= 1000) { // ignore sub-second noise
                if (now - lastExtendUpdate >= 5000) {
                    extendCurrentSession(now)
                    lastExtendUpdate = now
                }
            }
            return // don't start new session
        }

        // Package changed: close any previous session and start a new one
        if (currentPackage != null && packageName != currentPackage) {
            endCurrentSession(now)
        }

        val initialCategory = CategoryClassifier.classifyContent(screenTitle, packageName)
        startNewSession(packageName, screenTitle, initialCategory, now)
    }

    private fun packageName(): String = applicationContext.packageName

    // Overload: start with explicit category to support segmenting when category changes
    private fun startNewSession(packageName: String, screenTitle: String, category: String, startTime: Long) {
        Log.d(TAG, "Start session pkg=$packageName title=$screenTitle category=$category @${startTime}")
        currentPackage = packageName
        currentCategory = category
        sessionStart = startTime
        lastExtendUpdate = startTime

        val appLabel = PackageNameHelper.getAppLabel(this, packageName)

        val usageEvent = UsageEvent(
            packageName = packageName,
            appLabel = appLabel,
            screenTitle = screenTitle,
            category = category,
            startTime = startTime,
            endTime = startTime,
            durationMs = 0
        )

        serviceScope.launch {
            try {
                currentEventId = database.usageEventDao().insert(usageEvent)
                Log.d(TAG, "Inserted usage event id=$currentEventId for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert usage event", e)
            }
        }
    }

    private fun extendCurrentSession(now: Long) {
        val id = currentEventId ?: return
        if (sessionStart <= 0) return
        val duration = now - sessionStart
        if (duration < 500) return
        serviceScope.launch {
            try {
                database.usageEventDao().updateEventEnd(id, now, duration)
                Log.d(TAG, "Extend session id=$id duration=$duration ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extend session id=$id", e)
            }
        }
    }

    private fun endCurrentSession(now: Long = System.currentTimeMillis()) {
        val id = currentEventId ?: return
        if (sessionStart <= 0) return
        val duration = now - sessionStart
        if (duration < 300) { // discard ultra-short sessions
            Log.d(TAG, "Discard short session id=$id duration=$duration ms (<300ms)")
            currentEventId = null
            currentPackage = null
            currentCategory = null
            sessionStart = 0
            return
        }
        serviceScope.launch {
            try {
                database.usageEventDao().updateEventEnd(id, now, duration)
                Log.d(TAG, "End session id=$id duration=$duration ms (accepted=${duration >= 300})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to end session id=$id", e)
            }
        }
        currentEventId = null
        currentPackage = null
        currentCategory = null
        sessionStart = 0
    }

    // Add better text extraction logic
    private fun extractMeaningfulTitle(event: AccessibilityEvent, packageName: String): String {
        return when {
            packageName.contains("youtube", ignoreCase = true) -> extractYouTubeTitle(event)
            packageName.contains("instagram", ignoreCase = true) -> extractInstagramTitle(event)
            packageName.contains("chrome", ignoreCase = true) || packageName.contains("browser", ignoreCase = true) -> extractBrowserTitle(event)
            packageName.contains("incallui", ignoreCase = true) -> extractCallTitle(event)
            packageName.contains("whatsapp", ignoreCase = true) -> extractWhatsAppTitle(event)
            else -> extractGenericTitle(event)
        }
    }


    private fun extractYouTubeTitle(event: AccessibilityEvent): String {
        // Look for video title in specific YouTube elements
        val rootNode = rootInActiveWindow ?: return event.text?.toString() ?: ""

        // Search for video title elements
        val titleNodes = findNodesBySelector(rootNode, listOf(
            "android.widget.TextView", // Main title
            "android.view.ViewGroup" // Title container
        ))

        for (node in titleNodes) {
            val text = node.text?.toString()
            if (text != null && isValidYouTubeTitle(text)) {
                return text
            }
        }

        return event.text?.toString() ?: "YouTube Content"
    }

    private fun isValidYouTubeTitle(text: String): Boolean {
        // Filter out UI elements and suggestions
        val invalidPatterns = listOf(
            "Subscribe", "Like", "Share", "Comment",
            "Up next", "Autoplay", "Settings",
            "More videos", "Playlist"
        )

        return text.length > 10 &&
                !invalidPatterns.any { text.contains(it, ignoreCase = true) } &&
                !text.matches(Regex("\\d+:\\d+")) // Not duration
    }

    private fun extractCallTitle(event: AccessibilityEvent): String {
        val rootNode = rootInActiveWindow ?: return "Phone Call"

        // Look for contact name or phone number
        val contactNodes = findNodesBySelector(rootNode, listOf(
            "android.widget.TextView",
            "android.widget.EditText"
        ))

        for (node in contactNodes) {
            val text = node.text?.toString()
            if (text != null && isValidContactInfo(text)) {
                return "Call: $text"
            }
        }

        return "Phone Call"
    }

    private fun isValidContactInfo(text: String): Boolean {
        // Check if it's a valid contact name or phone number
        return text.length > 2 &&
                !text.contains("Call") &&
                !text.contains("End") &&
                !text.contains("Mute") &&
                !text.matches(Regex("\\d{1,2}:\\d{2}")) // Not call duration
    }

    private fun extractWhatsAppTitle(event: AccessibilityEvent): String {
        val text = event.text?.toString() ?: return "WhatsApp"

        // Filter out UI noise
        if (isWhatsAppUIElement(text)) {
            return "WhatsApp Chat"
        }

        // Extract chat name or meaningful content
        return when {
            text.contains(":") && text.length < 100 -> "Chat: ${text.substringBefore(":")}"
            text.length < 50 -> text
            else -> "WhatsApp Chat"
        }
    }

    private fun isWhatsAppUIElement(text: String): Boolean {
        val uiElements = listOf(
            "Voice message", "Double tap", "slide left", "slide up",
            "Type a message", "Camera", "Microphone"
        )
        return uiElements.any { text.contains(it, ignoreCase = true) }
    }

    private fun extractInstagramTitle(event: AccessibilityEvent): String {
        val text = event.text?.toString() ?: return "Instagram"

        // Filter out common Instagram UI elements
        if (isInstagramUIElement(text)) {
            return "Instagram Feed"
        }

        return when {
            text.contains("said") -> "Instagram Comment"
            text.contains("@") && text.length < 50 -> "Profile: $text"
            text.length < 100 -> text
            else -> "Instagram Feed"
        }
    }

    private fun isInstagramUIElement(text: String): Boolean {
        val uiElements = listOf(
            "Double tap to like", "likes", "ago", "Follow",
            "Following", "View profile", "Message"
        )
        return uiElements.any { text.contains(it, ignoreCase = true) }
    }

    private fun extractBrowserTitle(event: AccessibilityEvent): String {
        val rootNode = rootInActiveWindow ?: return "Web Page"

        // Look for page title in address bar or title element
        val titleNodes = findNodesBySelector(rootNode, listOf(
            "android.widget.EditText", // Address bar
            "android.widget.TextView"  // Page title
        ))

        for (node in titleNodes) {
            val text = node.text?.toString()
            if (text != null && isValidWebTitle(text)) {
                return text
            }
        }

        return event.text?.toString() ?: "Web Page"
    }

    private fun isValidWebTitle(text: String): Boolean {
        return text.length > 5 &&
                !text.startsWith("http") &&
                !text.contains("Search") &&
                !text.contains("Address bar")
    }

    private fun extractGenericTitle(event: AccessibilityEvent): String {
        val text = event.text?.toString() ?: return "Unknown Content"

        // Return first meaningful text that's not too long
        return when {
            text.length <= 100 -> text
            else -> text.substring(0, 97) + "..."
        }
    }

    private fun findNodesBySelector(root: AccessibilityNodeInfo, classNames: List<String>): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()

        fun traverse(node: AccessibilityNodeInfo) {
            if (classNames.contains(node.className?.toString())) {
                nodes.add(node)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { traverse(it) }
            }
        }

        traverse(root)
        return nodes
    }

    private fun isSystemNoise(text: String): Boolean {
        val noisePatterns = listOf(
            "Signal strength:", "Today:", "This month:", "MB", "GB", "5G+", "4G",
            "Double tap and hold", "Button.", "Expand", "Collapse",
            "Uninstalling will remove", "Star rating:", "Install",
            "More connectivity options", "Fingerprints, face data"
        )

        return noisePatterns.any { text.contains(it, ignoreCase = true) } ||
                text.matches(Regex("\\d+\\.\\d+\\s*(MB|GB)")) ||
                text.matches(Regex("\\d+ out of \\d+ bars?"))
    }

    override fun onInterrupt() { Log.d(TAG, "Interrupt"); endCurrentSession() }
    override fun onDestroy() { Log.d(TAG, "Destroy"); endCurrentSession(); serviceScope.cancel(); super.onDestroy() }
}