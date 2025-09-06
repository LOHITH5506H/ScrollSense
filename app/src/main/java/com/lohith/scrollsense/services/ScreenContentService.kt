package com.lohith.scrollsense.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScreenContentService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var repo: UsageRepository

    // For stability/debounce: require N identical detections before switching
    private var lastDetectedPkg: String? = null
    private var lastDetectedType: String? = null
    private var lastStableCount = 0
    private val REQUIRED_STABLE = 2 // require 2 consecutive detections
    private val EVAL_COOLDOWN_MS = 3000L
    private var lastEvalTs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val db = AppDatabase.get(this)
        repo = UsageRepository(db.contentSegmentDao())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val now = System.currentTimeMillis()

        // throttle evaluations to reduce CPU/battery
        if (now - lastEvalTs < EVAL_COOLDOWN_MS &&
            event.eventType !in importantEventTypes) return
        lastEvalTs = now

        val pkg = (event.packageName ?: return).toString()
        val root = rootInActiveWindow ?: return

        val detected = detectContentType(pkg, root)

        // Debounce / stability: only switch after REQUIRED_STABLE identical detections
        if (pkg == lastDetectedPkg && detected == lastDetectedType) {
            lastStableCount++
        } else {
            lastDetectedPkg = pkg
            lastDetectedType = detected
            lastStableCount = 1
        }

        if (lastStableCount >= REQUIRED_STABLE) {
            scope.launch {
                repo.maybeSwitch(pkg, detected, now)
            }
        }
    }

    override fun onInterrupt() {
        scope.launch { repo.stop(System.currentTimeMillis()) }
    }

    private val importantEventTypes = setOf(
        AccessibilityEvent.TYPE_VIEW_SCROLLED,
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        AccessibilityEvent.TYPE_VIEW_CLICKED,
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    )

    private fun detectContentType(pkg: String, root: AccessibilityNodeInfo): String {
        val textBlob = collectText(root).lowercase()

        // Package-level quick hints
        if (pkg.contains("youtube")) return "video"
        if (pkg.contains("vimeo")) return "video"
        if (pkg.contains("netflix")) return "video"
        if (pkg.contains("tiktok")) return "video"
        if (pkg.contains("instagram")) {
            if ("reel" in textBlob || "video" in textBlob) return "video"
        }
        if (pkg.contains("facebook")) {
            if ("watch" in textBlob || "reel" in textBlob) return "video"
        }

        // UI class heuristics
        if (hasAnyClass(root, listOf("VideoView","TextureView","SurfaceView","PlayerView","ExoPlayerView","android.widget.VideoView"))) {
            return "video"
        }

        // Keywords heuristics
        if (textBlob.contains("add to cart") || textBlob.contains("buy now") || textBlob.contains("₹") || textBlob.contains("price")) {
            return "shopping"
        }
        if (textBlob.contains("breaking") || textBlob.contains("headline") || textBlob.contains("news")) {
            return "news"
        }
        if (textBlob.contains("photo") || textBlob.contains("image") || textBlob.contains("gallery")) {
            return "image"
        }
        if (textBlob.contains("comment") || textBlob.contains("read more") || textBlob.length > 200) {
            // long text suggests article / reading
            return "text"
        }

        // Fallback to app-level mapping to avoid unknowns
        return when {
            pkg.contains("pinterest") -> "image"
            pkg.contains("amazon") || pkg.contains("flipkart") -> "shopping"
            else -> "other"
        }
    }

    private fun collectText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        fun dfs(n: AccessibilityNodeInfo?) {
            if (n == null) return
            n.text?.let { sb.append(it).append(' ') }
            for (i in 0 until n.childCount) dfs(n.getChild(i))
        }
        dfs(node)
        return sb.toString()
    }

    private fun hasAnyClass(node: AccessibilityNodeInfo?, classNames: List<String>): Boolean {
        if (node == null) return false
        var found = false
        fun dfs(n: AccessibilityNodeInfo?) {
            if (n == null || found) return
            val cls = n.className?.toString() ?: ""
            if (classNames.any { cls.contains(it, ignoreCase = true) }) {
                found = true; return
            }
            for (i in 0 until n.childCount) dfs(n.getChild(i))
        }
        dfs(node)
        return found
    }
}
