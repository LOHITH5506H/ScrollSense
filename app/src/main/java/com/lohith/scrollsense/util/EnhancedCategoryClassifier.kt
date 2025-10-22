package com.lohith.scrollsense.util

import android.content.Context
import android.util.Log
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.UserFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import org.json.JSONObject

/**
 * Enhanced content classifier that uses multiple strategies for better accuracy:
 * 1. Package-based classification (most reliable)
 * 2. ML-based text classification
 * 3. Keyword-based classification (fallback)
 * 4. User feedback learning system
 */
class EnhancedCategoryClassifier(private val context: Context) {

    companion object {
        private const val TAG = "EnhancedClassifier"

        // Known app package mappings for high accuracy
        private val PACKAGE_CATEGORY_MAP = mapOf(
            // Social
            "com.whatsapp" to "social",
            "com.facebook.katana" to "social",
            "com.instagram.android" to "social",
            "com.twitter.android" to "social",
            "com.snapchat.android" to "social",
            "com.linkedin.android" to "business",

            // Entertainment
            "com.google.android.youtube" to "entertainment",
            "com.netflix.mediaclient" to "entertainment",
            "com.amazon.avod.thirdpartyclient" to "entertainment",
            "com.hotstar.android" to "entertainment",
            "com.spotify.music" to "entertainment",

            // Games
            "com.supercell.clashofclans" to "games",
            "com.king.candycrushsaga" to "games",
            "com.pubg.imobile" to "games",
            "com.garena.game.freefire" to "games",

            // News
            "com.google.android.apps.magazines" to "news",
            "flipboard.app" to "news",
            "com.twitter.android" to "news", // Can be both social and news

            // Productivity
            "com.google.android.apps.docs.editors.docs" to "productivity",
            "com.microsoft.office.word" to "productivity",
            "com.google.android.gm" to "productivity",

            // Shopping
            "com.amazon.mShop.android.shopping" to "shopping",
            "com.flipkart.android" to "shopping",
            "com.myntra.android" to "shopping",

            // Communication/Calls
            "com.android.incallui" to "social",
            "com.android.dialer" to "social",
            "com.google.android.dialer" to "social",

            // Education
            "com.khanacademy.android" to "education",
            "com.coursera.android" to "education",
            "com.udemy.android" to "education"
        )

        // Content keywords with weights for better classification
        private val WEIGHTED_KEYWORDS = mapOf(
            "social" to mapOf(
                "chat" to 3.0f, "message" to 3.0f, "call" to 3.0f, "video call" to 4.0f,
                "friend" to 2.0f, "follow" to 2.0f, "like" to 1.5f, "share" to 1.5f,
                "comment" to 2.0f, "post" to 2.0f, "story" to 2.0f, "status" to 2.0f,
                "dialing" to 4.0f, "dialling" to 4.0f, "incoming" to 3.0f, "outgoing" to 3.0f
            ),
            "entertainment" to mapOf(
                "video" to 3.0f, "movie" to 4.0f, "series" to 4.0f, "episode" to 3.0f,
                "watch" to 2.0f, "play" to 2.0f, "stream" to 3.0f, "music" to 3.0f,
                "song" to 3.0f, "album" to 2.0f, "artist" to 2.0f, "playlist" to 2.0f
            ),
            "games" to mapOf(
                "level" to 3.0f, "score" to 3.0f, "game" to 4.0f, "play" to 2.0f,
                "battle" to 3.0f, "mission" to 3.0f, "quest" to 3.0f, "achievement" to 2.0f,
                "leaderboard" to 3.0f, "multiplayer" to 3.0f
            ),
            "news" to mapOf(
                "news" to 4.0f, "breaking" to 4.0f, "headline" to 3.0f, "article" to 3.0f,
                "politics" to 3.0f, "election" to 3.0f, "government" to 2.0f, "economy" to 2.0f
            ),
            "education" to mapOf(
                "learn" to 3.0f, "study" to 3.0f, "course" to 4.0f, "lesson" to 3.0f,
                "tutorial" to 3.0f, "education" to 4.0f, "knowledge" to 2.0f, "exam" to 3.0f
            ),
            "shopping" to mapOf(
                "buy" to 4.0f, "price" to 3.0f, "cart" to 4.0f, "order" to 3.0f,
                "purchase" to 4.0f, "sale" to 2.0f, "discount" to 2.0f, "deal" to 2.0f
            ),
            "productivity" to mapOf(
                "document" to 3.0f, "email" to 4.0f, "calendar" to 3.0f, "meeting" to 3.0f,
                "task" to 2.0f, "note" to 2.0f, "reminder" to 2.0f, "schedule" to 2.0f
            )
        )
    }

    private val userFeedbackDao by lazy {
        AppDatabase.getDatabase(context).userFeedbackDao()
    }

    // --- Adult veto (res/raw/veto_keywords_en.json) ---
    @Volatile private var adultVetoKeywords: Set<String>? = null

    private fun ensureAdultVetoLoaded(): Set<String> {
        adultVetoKeywords?.let { return it }
        val loaded = runCatching {
            val resId = context.resources.getIdentifier("veto_keywords_en", "raw", context.packageName)
            if (resId == 0) return@runCatching emptySet<String>()
            context.resources.openRawResource(resId).use { input ->
                val json = input.bufferedReader().readText()
                val obj = JSONObject(json)
                val arr = obj.optJSONArray("adult_veto") ?: return@use emptySet<String>()
                val out = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    out.add(arr.getString(i).lowercase(Locale.getDefault()))
                }
                out.toSet()
            }
        }.getOrElse { e ->
            Log.w(TAG, "Failed to load adult veto keywords", e)
            emptySet()
        }
        adultVetoKeywords = loaded
        return loaded
    }

    private fun containsAdultVeto(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase(Locale.getDefault())
        val veto = ensureAdultVetoLoaded()
        if (veto.isEmpty()) return false
        fun isAlnumOnly(s: String) = s.all { it.isLetterOrDigit() }
        for (token in veto) {
            if (token.isBlank()) continue
            if (isAlnumOnly(token)) {
                val pattern = ("\\b" + java.util.regex.Pattern.quote(token) + "\\b").toRegex(RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(lower)) return true
            } else {
                if (lower.contains(token)) return true
            }
        }
        return false
    }

    suspend fun classifyContent(
        screenText: String,
        packageName: String,
        previousCategory: String? = null
    ): ClassificationResult = withContext(Dispatchers.IO) {

        // Strategy 0: Adult veto — highest priority
        if (containsAdultVeto(screenText)) {
            return@withContext ClassificationResult(
                category = "adult",
                confidence = 0.98f,
                method = "adult-veto",
                subcategory = getSubcategory("adult", screenText)
            )
        }

        // Strategy 1: Package-based classification (highest confidence)
        PACKAGE_CATEGORY_MAP[packageName]?.let { category ->
            return@withContext ClassificationResult(
                category = category,
                confidence = 0.95f,
                method = "package-based",
                subcategory = getSubcategory(category, screenText)
            )
        }

        // Strategy 2: User feedback learning
        val userFeedback = userFeedbackDao.getFeedbackForPackage(packageName)
        userFeedback?.let { feedback ->
            if (feedback.confidence > 0.8f) {
                return@withContext ClassificationResult(
                    category = feedback.category,
                    confidence = feedback.confidence * 0.9f, // Slight reduction for learned data
                    method = "user-learned",
                    subcategory = getSubcategory(feedback.category, screenText)
                )
            }
        }

        // Strategy 3: Weighted keyword analysis
        val keywordResult = classifyByWeightedKeywords(screenText.lowercase())
        if (keywordResult.confidence > 0.6f) {
            return@withContext keywordResult
        }

        // Strategy 4: Context-based classification
        val contextResult = classifyByContext(screenText, packageName, previousCategory)
        if (contextResult.confidence > 0.5f) {
            return@withContext contextResult
        }

        // Fallback: Return "other" with low confidence
        ClassificationResult(
            category = "other",
            confidence = 0.3f,
            method = "fallback",
            subcategory = ""
        )
    }

    private fun classifyByWeightedKeywords(text: String): ClassificationResult {
        val categoryScores = mutableMapOf<String, Float>()

        WEIGHTED_KEYWORDS.forEach { (category, keywords) ->
            var score = 0.0f
            keywords.forEach { (keyword, weight) ->
                val occurrences = text.split(keyword).size - 1
                score += occurrences * weight
            }
            if (score > 0) {
                categoryScores[category] = score
            }
        }

        // --- NEW: add adult scoring from veto tokens as weighted features ---
        runCatching {
            val veto = ensureAdultVetoLoaded()
            if (veto.isNotEmpty()) {
                var adultScore = 0.0f
                val lower = text
                fun isAlnumOnly(s: String) = s.all { it.isLetterOrDigit() }
                for (token in veto) {
                    if (token.isBlank()) continue
                    val occurrences = if (isAlnumOnly(token)) {
                        ("\\b" + java.util.regex.Pattern.quote(token) + "\\b").toRegex(RegexOption.IGNORE_CASE)
                            .findAll(lower).count()
                    } else {
                        // simple contains -> approximate to 1 occurrence
                        if (lower.contains(token)) 1 else 0
                    }
                    adultScore += occurrences * 5.0f // strong weight for explicit signals
                }
                if (adultScore > 0) {
                    categoryScores["adult"] = maxOf(categoryScores["adult"] ?: 0f, adultScore)
                }
            }
        }

        val bestMatch = categoryScores.maxByOrNull { it.value }
        return if (bestMatch != null && bestMatch.value > 3.0f) {
            val confidence = minOf(bestMatch.value / 10.0f, 0.85f)
            ClassificationResult(
                category = bestMatch.key,
                confidence = confidence,
                method = "weighted-keywords",
                subcategory = getSubcategory(bestMatch.key, text)
            )
        } else {
            ClassificationResult("other", 0.3f, "keywords-low-score", "")
        }
    }

    private fun classifyByContext(
        text: String,
        packageName: String,
        previousCategory: String?
    ): ClassificationResult {
        // Use app package name patterns for additional context
        val packagePatterns = mapOf(
            "game" to "games",
            "social" to "social",
            "news" to "news",
            "music" to "entertainment",
            "video" to "entertainment",
            "shop" to "shopping",
            "learn" to "education"
        )

        packagePatterns.forEach { (pattern, category) ->
            if (packageName.contains(pattern, ignoreCase = true)) {
                return ClassificationResult(
                    category = category,
                    confidence = 0.7f,
                    method = "package-pattern",
                    subcategory = getSubcategory(category, text)
                )
            }
        }

        // If we have a previous category and current text is ambiguous,
        // continue with previous category but lower confidence
        previousCategory?.let { prev ->
            if (text.length < 50) { // Short text, likely continuation
                return ClassificationResult(
                    category = prev,
                    confidence = 0.5f,
                    method = "context-continuation",
                    subcategory = getSubcategory(prev, text)
                )
            }
        }

        return ClassificationResult("other", 0.2f, "context-unknown", "")
    }

    private fun getSubcategory(category: String, text: String): String {
        return when (category) {
            "adult" -> when {
                text.contains("webcam", ignoreCase = true) -> "webcam"
                text.contains("video", ignoreCase = true) -> "videos"
                text.contains("photo", ignoreCase = true) || text.contains("image", ignoreCase = true) || text.contains("gallery", ignoreCase = true) -> "photos"
                else -> "general_adult"
            }
            "social" -> when {
                text.contains("call", ignoreCase = true) -> "voice_calls"
                text.contains("video", ignoreCase = true) -> "video_calls"
                text.contains("message", ignoreCase = true) -> "messaging"
                else -> "general_social"
            }
            "entertainment" -> when {
                text.contains("music", ignoreCase = true) -> "music"
                text.contains("video", ignoreCase = true) -> "video"
                text.contains("movie", ignoreCase = true) -> "movies"
                else -> "general_entertainment"
            }
            "games" -> when {
                text.contains("puzzle", ignoreCase = true) -> "puzzle"
                text.contains("action", ignoreCase = true) -> "action"
                text.contains("strategy", ignoreCase = true) -> "strategy"
                else -> "general_games"
            }
            else -> ""
        }
    }

    // Method to record user feedback for learning
    suspend fun recordUserFeedback(
        packageName: String,
        correctedCategory: String,
        originalCategory: String
    ) = withContext(Dispatchers.IO) {
        try {
            userFeedbackDao.insertOrUpdate(
                UserFeedback(
                    packageName = packageName,
                    category = correctedCategory,
                    confidence = 0.9f,
                    feedbackCount = 1,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            Log.d(TAG, "Recorded user feedback: $packageName -> $correctedCategory")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording user feedback", e)
        }
    }
}

data class ClassificationResult(
    val category: String,
    val confidence: Float,
    val method: String,
    val subcategory: String
)
