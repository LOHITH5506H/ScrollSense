package com.lohith.scrollsense.util

import android.content.Context
import android.util.Log
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.UserFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.regex.Pattern
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
        // MODIFIED: Updated categories to match your new JSON file
        private val PACKAGE_CATEGORY_MAP = mapOf(
            // Social
            "com.whatsapp" to "social",
            "com.facebook.katana" to "social",
            "com.instagram.android" to "social",
            "com.twitter.android" to "social",
            "com.snapchat.android" to "social",
            "com.linkedin.android" to "business", // Changed from social

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

            // Productivity & Business
            "com.google.android.apps.docs.editors.docs" to "business", // Changed from productivity
            "com.microsoft.office.word" to "business", // Changed from productivity
            "com.google.android.gm" to "business", // Changed from productivity

            // Shopping
            "com.amazon.mShop.android.shopping" to "Shopping", // Match JSON case
            "com.flipkart.android" to "Shopping",
            "com.myntra.android" to "Shopping",

            // --- NEW: Added Finance packages ---
            "zebpay.Application" to "Finance",
            "com.google.android.apps.nbu.paisa.user" to "Finance", // Google Pay
            "com.phonepe.app" to "Finance", // PhonePe
            "net.one97.paytm" to "Finance", //Paytm

            // Communication/Calls
            "com.android.incallui" to "social",
            "com.android.dialer" to "social",
            "com.google.android.dialer" to "social",

            // Education
            "com.khanacademy.android" to "education",
            "com.coursera.android" to "education",
            "com.udemy.android" to "education"
        )

        // KEPT: This is your existing functionality
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
            // Note: Your JSON has "Shopping" (capital S). We will align to that.
            "shopping" to mapOf(
                "buy" to 4.0f, "price" to 3.0f, "cart" to 4.0f, "order" to 3.0f,
                "purchase" to 4.0f, "sale" to 2.0f, "discount" to 2.0f, "deal" to 2.0f
            ),
            // Note: Your JSON has "business".
            "productivity" to mapOf(
                "document" to 3.0f, "email" to 4.0f, "calendar" to 3.0f, "meeting" to 3.0f,
                "task" to 2.0f, "note" to 2.0f, "reminder" to 2.0f, "schedule" to 2.0f
            )
        )
    }

    private val userFeedbackDao by lazy {
        AppDatabase.getDatabase(context).userFeedbackDao()
    }

    // KEPT: This is your existing functionality for adult veto
    @Volatile private var adultVetoKeywords: Set<String>? = null

    // KEPT: This is your existing functionality
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

    // KEPT: This is your existing functionality
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

    // --- NEW: Load keywords dynamically from keywords_en.json ---
    private val loadedJsonKeywords: Map<String, List<Pattern>> by lazy {
        Log.d(TAG, "Loading keywords from KeywordLoader...")
        val allKeywords = KeywordLoader.loadAll(context)
        // Load only the "en" keywords
        val enKeywords = allKeywords.mapValues { (_, langMap) ->
            langMap["en"] ?: emptyList()
        }.filterValues { it.isNotEmpty() }
        Log.d(TAG, "Loaded ${enKeywords.size} JSON categories: ${enKeywords.keys.joinToString(", ")}")

        // Pre-compile regex patterns for efficiency
        enKeywords.mapValues { (_, keywords) ->
            keywords.map {
                Pattern.compile("\\b${Pattern.quote(it)}\\b", Pattern.CASE_INSENSITIVE)
            }
        }
    }


    suspend fun classifyContent(
        screenText: String,
        packageName: String,
        previousCategory: String? = null
    ): ClassificationResult = withContext(Dispatchers.IO) {

        // KEPT: Strategy 0 (Your existing functionality)
        if (containsAdultVeto(screenText)) {
            return@withContext ClassificationResult(
                category = "adult",
                confidence = 0.98f,
                method = "adult-veto",
                subcategory = getSubcategory("adult", screenText)
            )
        }

        // KEPT: Strategy 1 (Your existing functionality)
        PACKAGE_CATEGORY_MAP[packageName]?.let { category ->
            return@withContext ClassificationResult(
                category = category,
                confidence = 0.95f,
                method = "package-based",
                subcategory = getSubcategory(category, screenText)
            )
        }

        // KEPT: Strategy 2 (Your existing functionality)
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

        // MODIFIED: Strategy 3 (Combines old and new keyword logic)
        val keywordResult = classifyByHybridKeywords(screenText.lowercase())
        if (keywordResult.confidence > 0.6f) {
            return@withContext keywordResult
        }

        // KEPT: Strategy 4 (Your existing functionality)
        val contextResult = classifyByContext(screenText, packageName, previousCategory)
        if (contextResult.confidence > 0.5f) {
            return@withContext contextResult
        }

        // KEPT: Fallback (Your existing functionality)
        ClassificationResult(
            category = "other",
            confidence = 0.3f,
            method = "fallback",
            subcategory = ""
        )
    }

    /**
     * MODIFIED: Renamed to `classifyByHybridKeywords`
     * This function now combines your old `WEIGHTED_KEYWORDS`
     * with the new `loadedJsonKeywords` from `keywords_en.json`.
     */
    private fun classifyByHybridKeywords(text: String): ClassificationResult {
        val categoryScores = mutableMapOf<String, Float>()

        // --- PART 1: Keep your existing WEIGHTED_KEYWORDS logic ---
        WEIGHTED_KEYWORDS.forEach { (category, keywords) ->
            var score = 0.0f
            keywords.forEach { (keyword, weight) ->
                // This is your old, less accurate "split" logic, kept as requested
                val occurrences = text.split(keyword).size - 1
                score += occurrences * weight
            }
            if (score > 0) {
                // We must align category names, e.g., "shopping" -> "Shopping"
                val categoryKey = when(category) {
                    "shopping" -> "Shopping"
                    "productivity" -> "business" // Re-map productivity to business
                    else -> category
                }
                categoryScores[categoryKey] = (categoryScores[categoryKey] ?: 0f) + score
            }
        }

        // --- PART 2: Keep your existing adult scoring logic ---
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
                        if (lower.contains(token)) 1 else 0
                    }
                    adultScore += occurrences * 5.0f // strong weight
                }
                if (adultScore > 0) {
                    categoryScores["adult"] = maxOf(categoryScores["adult"] ?: 0f, adultScore)
                }
            }
        }

        // --- PART 3: ADD new logic for keywords_en.json ---
        // This will find "Finance", "Business", "Fitness", etc.
        // It also reinforces scores for "social", "adult", etc.
        loadedJsonKeywords.forEach { (category, patterns) ->
            var score = 0.0f
            patterns.forEach { pattern ->
                val matcher = pattern.matcher(text)
                while (matcher.find()) {
                    score += 1.5f // Add a standard score for each match
                }
            }
            if (score > 0) {
                // Add this score to any existing score
                categoryScores[category] = (categoryScores[category] ?: 0f) + score
            }
        }

        // --- Find the best match from the COMBINED scores ---
        val bestMatch = categoryScores.maxByOrNull { it.value }

        // Use your existing threshold
        return if (bestMatch != null && bestMatch.value > 3.0f) {
            val confidence = (bestMatch.value / 10.0f).coerceIn(0.6f, 0.9f)
            ClassificationResult(
                category = bestMatch.key,
                confidence = confidence,
                method = "hybrid-keywords",
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
        // MODIFIED: Added new patterns
        val packagePatterns = mapOf(
            "game" to "games",
            "social" to "social",
            "news" to "news",
            "music" to "entertainment",
            "video" to "entertainment",
            "shop" to "Shopping",
            "learn" to "education",
            "bank" to "Finance",
            "pay" to "Finance",
            "trade" to "Finance",
            "wallet" to "Finance",
            "fitness" to "fitness",
            "health" to "fitness"
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

    // MODIFIED: Added "Finance" subcategory
    private fun getSubcategory(category: String, text: String): String {
        val lowerText = text.lowercase(Locale.getDefault())
        return when (category.lowercase(Locale.getDefault())) {
            "adult" -> when {
                lowerText.contains("webcam") -> "webcam"
                lowerText.contains("video") -> "videos"
                lowerText.contains("photo") || lowerText.contains("image") || lowerText.contains("gallery") -> "photos"
                else -> "general_adult"
            }
            "social" -> when {
                lowerText.contains("call") -> "voice_calls"
                lowerText.contains("video") -> "video_calls"
                lowerText.contains("message") -> "messaging"
                else -> "general_social"
            }
            "entertainment" -> when {
                lowerText.contains("music") -> "music"
                lowerText.contains("video") -> "video"
                lowerText.contains("movie") -> "movies"
                else -> "general_entertainment"
            }
            "games" -> when {
                lowerText.contains("puzzle") -> "puzzle"
                lowerText.contains("action") -> "action"
                lowerText.contains("strategy") -> "strategy"
                else -> "general_games"
            }
            "finance" -> when {
                lowerText.contains("otp") || lowerText.contains("verification") -> "verification"
                lowerText.contains("payment") || lowerText.contains("upi") -> "payment"
                lowerText.contains("trade") || lowerText.contains("crypto") -> "trading"
                else -> "general_finance"
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