package com.lohith.scrollsense.services

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import com.lohith.scrollsense.BuildConfig
import com.lohith.scrollsense.data.models.AppUsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class GeminiAIService(private val context: Context) {

    companion object {
        private const val TAG = "GeminiAIService"
        private const val MODEL_NAME = "gemini-1.5-flash"

        // Predefined categories for common apps to reduce API calls
        private val PREDEFINED_CATEGORIES = mapOf(
            // Social Media
            "com.facebook.katana" to "Social Media",
            "com.instagram.android" to "Social Media",
            "com.twitter.android" to "Social Media",
            "com.snapchat.android" to "Social Media",
            "com.linkedin.android" to "Social Media",
            "com.pinterest" to "Social Media",
            "com.reddit.frontpage" to "Social Media",
            "com.discord" to "Social Media",
            "com.tiktok_app" to "Social Media",

            // Communication
            "com.whatsapp" to "Communication",
            "com.telegram.messenger" to "Communication",
            "org.thoughtcrime.securesms" to "Communication",
            "com.viber.voip" to "Communication",
            "com.skype.raider" to "Communication",

            // Entertainment
            "com.netflix.mediaclient" to "Entertainment",
            "com.google.android.youtube" to "Entertainment",
            "com.amazon.avod.thirdpartyclient" to "Entertainment",
            "com.disney.disneyplus" to "Entertainment",
            "com.hulu.plus" to "Entertainment",
            "in.startv.hotstar" to "Entertainment",

            // Music
            "com.spotify.music" to "Music",
            "com.google.android.music" to "Music",
            "com.apple.android.music" to "Music",
            "com.amazon.mp3" to "Music",
            "com.gaana" to "Music",
            "com.jio.media.jiobeats" to "Music",

            // Games
            "com.supercell.clashofclans" to "Games",
            "com.king.candycrushsaga" to "Games",
            "com.roblox.client" to "Games",
            "com.mojang.minecraftpe" to "Games",
            "com.pubg.imobile" to "Games",
            "com.garena.game.freefire" to "Games",

            // Productivity
            "com.microsoft.office.outlook" to "Productivity",
            "com.google.android.gm" to "Productivity",
            "com.microsoft.office.word" to "Productivity",
            "com.google.android.apps.docs.editors.docs" to "Productivity",
            "com.dropbox.android" to "Productivity",
            "com.evernote" to "Productivity",
            "com.notion.id" to "Productivity",
            "com.slack" to "Productivity",

            // Shopping
            "com.amazon.mShop.android.shopping" to "Shopping",
            "com.flipkart.android" to "Shopping",
            "com.myntra.android" to "Shopping",
            "com.contextlogic.wish" to "Shopping",
            "com.ebay.mobile" to "Shopping",

            // News & Reading
            "com.google.android.apps.magazines" to "News & Reading",
            "flipboard.app" to "News & Reading",
            "com.cnn.mobile.android.phone" to "News & Reading",
            "com.inshorts.app" to "News & Reading",

            // Health & Fitness
            "com.fitbit.FitbitMobile" to "Health & Fitness",
            "com.myfitnesspal.android" to "Health & Fitness",
            "com.nike.plusone" to "Health & Fitness",
            "com.google.android.apps.fitness" to "Health & Fitness",

            // Navigation
            "com.google.android.apps.maps" to "Navigation",
            "com.waze" to "Navigation",
            "com.here.app.maps" to "Navigation",
            "com.olacabs.customer" to "Navigation",
            "com.ubercab" to "Navigation",

            // Photography
            "com.adobe.photoshop.express" to "Photography",
            "com.vsco.cam" to "Photography",
            "com.google.android.apps.photos" to "Photography",
            "com.instagram.layout" to "Photography",

            // Finance
            "com.phonepe.app" to "Finance",
            "net.one97.paytm" to "Finance",
            "com.google.android.apps.nbu.paisa.user" to "Finance",
            "com.sbi.lotusintouch" to "Finance",
            "com.application.zomato" to "Food & Drink"
        )
    }

    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.1f
                topK = 1
                topP = 0.8f
                maxOutputTokens = 50
            }
        )
    }

    // Simple heuristic to decide if remote calls should be attempted
    private fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key.length >= 20 && !key.equals("YOUR_API_KEY", ignoreCase = true)
    }

    // Cache for AI categorization results
    private val categoryCache = ConcurrentHashMap<String, String>()

    suspend fun categorizeApps(apps: List<AppUsageData>): List<AppUsageData> = withContext(Dispatchers.IO) {
        val categorizedApps = mutableListOf<AppUsageData>()

        for (app in apps) {
            val category = categorizeApp(app.packageName, app.appName)
            categorizedApps.add(app.copy(category = category))
        }

        return@withContext categorizedApps
    }

    private suspend fun categorizeApp(packageName: String, appName: String): String {
        // Check predefined categories first
        PREDEFINED_CATEGORIES[packageName]?.let { predefinedCategory ->
            categoryCache[packageName] = predefinedCategory
            return predefinedCategory
        }

        // Check cache
        categoryCache[packageName]?.let { cachedCategory ->
            return cachedCategory
        }

        // Use AI for unknown apps
        return try {
            val category = categorizeWithAI(packageName, appName)
            categoryCache[packageName] = category
            category
        } catch (e: Exception) {
            Log.e(TAG, "AI categorization failed for $packageName", e)
            val fallbackCategory = getFallbackCategory(appName, packageName)
            categoryCache[packageName] = fallbackCategory
            fallbackCategory
        }
    }

    private suspend fun categorizeWithAI(packageName: String, appName: String): String = withContext(Dispatchers.IO) {
        // Short-circuit if API key isn't configured to avoid noisy failures
        if (!isApiKeyConfigured()) {
            throw IllegalStateException("Gemini API key not configured")
        }
        val prompt = createCategorizationPrompt(packageName, appName)

        try {
            val response: GenerateContentResponse = generativeModel.generateContent(prompt)
            val responseText = response.text?.trim() ?: ""

            return@withContext parseCategory(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            throw e
        }
    }

    private fun createCategorizationPrompt(packageName: String, appName: String): String {
        return """
            Categorize this mobile app into exactly ONE of these categories:
            Social Media, Entertainment, Games, Productivity, Education, Shopping, News & Reading, 
            Health & Fitness, Navigation, Photography, Music, Communication, Finance, Food & Drink, 
            Travel, Lifestyle, Business, Sports, Weather, Utilities
            
            App Name: $appName
            Package: $packageName
            
            Rules:
            - Respond with ONLY the category name
            - Use exact category names from the list above
            - If uncertain, choose the most likely category
            - For system or unknown apps, use "Utilities"
            
            Category:
        """.trimIndent()
    }

    private fun parseCategory(aiResponse: String): String {
        val validCategories = setOf(
            "Social Media", "Entertainment", "Games", "Productivity", "Education",
            "Shopping", "News & Reading", "Health & Fitness", "Navigation",
            "Photography", "Music", "Communication", "Finance", "Food & Drink",
            "Travel", "Lifestyle", "Business", "Sports", "Weather", "Utilities"
        )

        // Clean the response
        val cleanResponse = aiResponse.replace(Regex("[^a-zA-Z0-9\\s&]"), "").trim()

        // Find exact match
        validCategories.find { it.equals(cleanResponse, ignoreCase = true) }?.let {
            return it
        }

        // Find partial match
        validCategories.find { category ->
            cleanResponse.contains(category, ignoreCase = true) ||
                    category.contains(cleanResponse, ignoreCase = true)
        }?.let {
            return it
        }

        return "Utilities"
    }

    private fun getFallbackCategory(appName: String, packageName: String): String {
        val lowerAppName = appName.lowercase()
        val lowerPackageName = packageName.lowercase()

        return when {
            lowerAppName.contains("game") || lowerPackageName.contains("game") -> "Games"
            lowerAppName.contains("music") || lowerAppName.contains("spotify") ||
                    lowerAppName.contains("audio") -> "Music"
            lowerAppName.contains("photo") || lowerAppName.contains("camera") ||
                    lowerAppName.contains("gallery") -> "Photography"
            lowerAppName.contains("social") || lowerAppName.contains("facebook") ||
                    lowerAppName.contains("instagram") || lowerAppName.contains("twitter") -> "Social Media"
            lowerAppName.contains("news") || lowerAppName.contains("read") -> "News & Reading"
            lowerAppName.contains("shop") || lowerAppName.contains("buy") ||
                    lowerAppName.contains("amazon") || lowerAppName.contains("flipkart") -> "Shopping"
            lowerAppName.contains("fitness") || lowerAppName.contains("health") -> "Health & Fitness"
            lowerAppName.contains("map") || lowerAppName.contains("navigation") ||
                    lowerAppName.contains("uber") || lowerAppName.contains("ola") -> "Navigation"
            lowerAppName.contains("video") || lowerAppName.contains("netflix") ||
                    lowerAppName.contains("youtube") -> "Entertainment"
            lowerAppName.contains("chat") || lowerAppName.contains("message") ||
                    lowerAppName.contains("whatsapp") || lowerAppName.contains("telegram") -> "Communication"
            lowerAppName.contains("bank") || lowerAppName.contains("pay") ||
                    lowerAppName.contains("finance") -> "Finance"
            lowerAppName.contains("food") || lowerAppName.contains("zomato") ||
                    lowerAppName.contains("swiggy") -> "Food & Drink"
            else -> "Utilities"
        }
    }

    fun clearCache() {
        categoryCache.clear()
    }

    fun getCacheSize(): Int = categoryCache.size

    fun isCached(packageName: String): Boolean = categoryCache.containsKey(packageName)

    fun getCachedCategories(): Map<String, String> = categoryCache.toMap()
}