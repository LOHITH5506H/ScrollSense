package com.lohith.scrollsense

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.UsageEvent
import com.lohith.scrollsense.util.CategoryClassifier
import com.lohith.scrollsense.util.PackageNameHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MyAccessibilityService"

class MyAccessibilityService : AccessibilityService() {

    private var currentPackage: String? = null
    private var currentTitleText: String? = null
    private var currentStart: Long = 0L
    private var lastEventTime: Long = 0L

    private val mainScope = CoroutineScope(Dispatchers.IO)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 500
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        // Skip useless system packages
        if (pkg in listOf("com.android.systemui", "com.google.android.googlequicksearchbox", packageName)) {
            return
        }

        val now = System.currentTimeMillis()

        // Extract text using improved method
        val title = extractTextFromEvent(event)

        // DEBUG LOG - ADD THIS TO SEE WHAT'S BEING EXTRACTED
        Log.d(TAG, "🔍 EXTRACTION DEBUG:")
        Log.d(TAG, "   Package: $pkg")
        Log.d(TAG, "   Extracted title: '$title'")

        if (pkg != currentPackage) {
            // App switched → close previous
            saveCurrentEvent(now)

            // Start new
            currentPackage = pkg
            currentTitleText = title
            currentStart = now
        } else {
            // Same app still active → update title if we found better text
            if (title.isNotEmpty() && title != currentTitleText && title.length > (currentTitleText?.length ?: 0)) {
                Log.d(TAG, "Updated title for $pkg: '$title'")
                currentTitleText = title
            }
            lastEventTime = now
        }
    }

    override fun onInterrupt() {
        saveCurrentEvent(System.currentTimeMillis())
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        saveCurrentEvent(System.currentTimeMillis())
        return super.onUnbind(intent)
    }

    private fun saveCurrentEvent(endTime: Long) {
        if (currentPackage != null && currentStart > 0) {
            val duration = endTime - currentStart

            if (duration > 5000) {
                val screenTitle = currentTitleText ?: ""
                val category = CategoryClassifier.classify(screenTitle)

                // DEBUG LOG FOR CLASSIFICATION
                Log.d(TAG, "🎯 CLASSIFICATION DEBUG:")
                Log.d(TAG, "   Title: '$screenTitle'")
                Log.d(TAG, "   Category: '$category'")
                Log.d(TAG, "   Duration: ${duration / 1000}s")

                val ev = UsageEvent(
                    id = 0,
                    packageName = currentPackage!!,
                    appLabel = PackageNameHelper.getAppLabel(applicationContext, currentPackage!!),
                    screenTitle = screenTitle,
                    category = category,
                    startTime = currentStart,
                    endTime = endTime,
                    durationMs = duration
                )
                persistEvent(ev)
            }
        }
        currentPackage = null
        currentTitleText = null
        currentStart = 0L
    }

    private fun persistEvent(ev: UsageEvent) {
        mainScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                db.usageEventDao().insert(ev)
                Log.d(TAG, "Saved event: ${ev.appLabel} | ${ev.screenTitle} | ${ev.category} | ${ev.durationMs / 1000}s")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist event", e)
            }
        }
    }

    private fun extractTextFromEvent(event: AccessibilityEvent): String {
        val pkg = event.packageName?.toString() ?: ""

        // First try to get text from the event itself
        val eventText = extractEventText(event)
        if (eventText.isNotEmpty() && !isSystemUIText(eventText)) {
            return eventText
        }

        // For YouTube specifically, try to get the root window and traverse
        if (pkg == "com.google.android.youtube") {
            val rootNode = getRootInActiveWindow()
            if (rootNode != null) {
                val youtubeTitle = extractYouTubeTitle(rootNode)
                if (youtubeTitle.isNotEmpty()) {
                    return youtubeTitle
                }
            }
        }

        // For other apps, try to extract from event source
        val sourceNode = event.source
        if (sourceNode != null) {
            val allTexts = extractAllTextsFromNode(sourceNode)
            val bestText = findBestTitle(allTexts, pkg)
            if (bestText.isNotEmpty()) {
                return bestText
            }
        }

        // If all else fails, try root window traversal
        val rootNode = getRootInActiveWindow()
        if (rootNode != null) {
            val allTexts = extractAllTextsFromNode(rootNode)
            val bestText = findBestTitle(allTexts, pkg)
            if (bestText.isNotEmpty()) {
                return bestText
            }
        }

        return eventText // Fallback to event text even if it might be system UI
    }

    private fun extractEventText(event: AccessibilityEvent): String {
        val title = event.contentDescription?.toString()
        if (!title.isNullOrBlank()) return title

        val textParts = StringBuilder()
        event.text?.forEach { c -> textParts.append(c).append(" ") }
        val txt = textParts.toString().trim()
        if (txt.isNotEmpty()) return txt

        return event.className?.toString() ?: ""
    }

    private fun extractYouTubeTitle(node: AccessibilityNodeInfo): String {
        val allTexts = extractAllTextsFromNode(node)

        // Look for YouTube-specific patterns
        for (text in allTexts) {
            if (isLikelyVideoTitle(text)) {
                Log.d(TAG, "Found YouTube title: '$text'")
                return text
            }
        }

        return ""
    }

    private fun extractAllTextsFromNode(node: AccessibilityNodeInfo?): List<String> {
        val texts = mutableListOf<String>()

        if (node == null) return texts

        try {
            // Get text from current node
            node.text?.let { if (it.isNotBlank()) texts.add(it.toString()) }
            node.contentDescription?.let { if (it.isNotBlank()) texts.add(it.toString()) }

            // Recursively get text from children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    texts.addAll(extractAllTextsFromNode(child))
                    child.recycle() // Important: recycle to avoid memory leaks
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting texts from node", e)
        }

        return texts
    }

    private fun findBestTitle(texts: List<String>, packageName: String): String {
        if (texts.isEmpty()) return ""

        // Filter out system UI and short texts
        val filteredTexts = texts.filter { text ->
            !isSystemUIText(text) &&
                    text.length > 5 &&
                    text.length < 150 && // Reasonable title length
                    !text.matches(Regex("^[0-9:]+$")) && // Not just timestamps
                    !text.matches(Regex("^[0-9]+$")) // Not just numbers
        }

        if (filteredTexts.isEmpty()) return ""

        // For YouTube, prioritize texts that look like video titles
        if (packageName == "com.google.android.youtube") {
            filteredTexts.find { isLikelyVideoTitle(it) }?.let { return it }
        }

        // Return the longest meaningful text as it's most likely to be a title
        return filteredTexts.maxByOrNull { it.length } ?: filteredTexts.first()
    }

    private fun isLikelyVideoTitle(text: String): Boolean {
        // YouTube titles are usually between 15-100 characters
        if (text.length < 15 || text.length > 100) return false

        // Exclude common UI elements (more comprehensive list)
        val excludePatterns = listOf(
            "subscribe", "notification", "menu", "search", "home", "library",
            "liked", "watch later", "history", "settings", "help", "send feedback",
            "youtube", "google", "sign in", "create", "upload", "shorts", "live",
            "comments", "like", "share", "download", "playlist", "channel",
            "views", "ago", "premium", "music", "kids", "tv", "studio",
            "recommended", "trending", "subscriptions", "browse"
        )

        val lowerText = text.lowercase()
        if (excludePatterns.any { lowerText.contains(it) }) return false

        // Good indicators of video titles
        return text.any { it.isLetter() } && // Contains letters
                !text.startsWith("http") && // Not a URL
                !text.contains("@") && // Not an email
                text.split(" ").size >= 3 && // At least 3 words
                !text.all { it.isDigit() || it.isWhitespace() || it in ":-" } // Not just time/numbers
    }

    private fun isSystemUIText(text: String): Boolean {
        val systemTexts = listOf(
            "system ui", "systemui", "navigation bar", "status bar", "notification",
            "back", "home", "recent", "menu", "search", "keyboard", "ime",
            "android", "launcher", "quicksettings", "recents", "overview"
        )

        val lowerText = text.lowercase()
        return systemTexts.any { lowerText.contains(it) } ||
                text.length < 3 ||
                text.matches(Regex("^[^a-zA-Z0-9]*$")) // Only special characters
    }
}
