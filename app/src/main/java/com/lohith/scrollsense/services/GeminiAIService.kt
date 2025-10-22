package com.lohith.scrollsense.services

import android.content.Context
import com.lohith.scrollsense.data.models.AppUsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// This class is now an OFFLINE service for categorizing apps based on package names.
class GeminiAIService(private val context: Context) {

    companion object {
        // Cache for categorization results
        private val categoryCache = ConcurrentHashMap<String, String>()

        // Expanded predefined categories for common apps to work offline
        private val PREDEFINED_CATEGORIES = mapOf(
            // Social Media
            "com.facebook.katana" to "Social Media", "com.facebook.lite" to "Social Media",
            "com.instagram.android" to "Social Media", "com.twitter.android" to "Social Media",
            "com.snapchat.android" to "Social Media", "com.linkedin.android" to "Social Media",
            "com.pinterest" to "Social Media", "com.reddit.frontpage" to "Social Media",
            "com.discord" to "Social Media", "com.zhiliaoapp.musically" to "Social Media", // TikTok
            "com.tumblr" to "Social Media", "com.quora.android" to "Social Media",

            // Communication
            "com.whatsapp" to "Communication", "com.telegram.messenger" to "Communication",
            "org.thoughtcrime.securesms" to "Communication", // Signal
            "com.viber.voip" to "Communication", "com.skype.raider" to "Communication",
            "com.google.android.apps.messaging" to "Communication", "com.truecaller" to "Communication",
            "com.google.android.gm" to "Communication", "com.microsoft.office.outlook" to "Communication",
            "com.google.android.talk" to "Communication", // Hangouts

            // Entertainment
            "com.netflix.mediaclient" to "Entertainment", "com.google.android.youtube" to "Entertainment",
            "com.amazon.avod.thirdpartyclient" to "Entertainment", // Prime Video
            "com.disney.disneyplus" to "Entertainment", "com.hulu.plus" to "Entertainment",
            "in.startv.hotstar" to "Entertainment", "tv.twitch.android.app" to "Entertainment",
            "com.google.android.videos" to "Entertainment", // Google TV
            "com.mxtech.videoplayer.ad" to "Entertainment", // MX Player
            "org.videolan.vlc" to "Entertainment", // VLC

            // Music
            "com.spotify.music" to "Music", "com.google.android.music" to "Music",
            "com.apple.android.music" to "Music", "com.amazon.mp3" to "Music",
            "com.gaana" to "Music", "com.jio.media.jiobeats" to "Music", // JioSaavn
            "com.shazam.android" to "Music", "com.soundcloud.android" to "Music",

            // Games
            "com.supercell.clashofclans" to "Games", "com.king.candycrushsaga" to "Games",
            "com.roblox.client" to "Games", "com.mojang.minecraftpe" to "Games",
            "com.pubg.imobile" to "Games", // BGMI
            "com.garena.game.freefire" to "Games", "com.activision.callofduty.shooter" to "Games",
            "com.ludo.king" to "Games", "com.kiloo.subwaysurf" to "Games",

            // Productivity
            "com.google.android.apps.docs" to "Productivity", "com.microsoft.office.word" to "Productivity",
            "com.dropbox.android" to "Productivity", "com.evernote" to "Productivity",
            "com.notion.id" to "Productivity", "com.slack" to "Productivity",
            "com.google.android.calendar" to "Productivity", "com.anydo" to "Productivity",
            "org.mozilla.firefox" to "Productivity", "com.android.chrome" to "Productivity",

            // Shopping
            "com.amazon.mShop.android.shopping" to "Shopping", "com.flipkart.android" to "Shopping",
            "com.myntra.android" to "Shopping", "com.contextlogic.wish" to "Shopping",
            "com.ebay.mobile" to "Shopping", "in.meeso.supply" to "Shopping",

            // News & Reading
            "com.google.android.apps.magazines" to "News & Reading", "flipboard.app" to "News & Reading",
            "com.cnn.mobile.android.phone" to "News & Reading", "com.inshorts.app" to "News & Reading",
            "com.google.android.apps.books" to "News & Reading", "com.amazon.kindle" to "News & Reading",

            // Health & Fitness
            "com.fitbit.FitbitMobile" to "Health & Fitness", "com.myfitnesspal.android" to "Health & Fitness",
            "com.nike.plusone" to "Health & Fitness", "com.google.android.apps.fitness" to "Health & Fitness",

            // Navigation & Travel
            "com.google.android.apps.maps" to "Navigation", "com.waze" to "Navigation",
            "com.olacabs.customer" to "Travel", "com.ubercab" to "Travel",
            "com.makemytrip" to "Travel", "com.goibibo" to "Travel",

            // Photography
            "com.adobe.lrmobile" to "Photography", "com.vsco.cam" to "Photography", // VSCO
            "com.google.android.apps.photos" to "Photography", "com.picsart.studio" to "Photography",

            // Finance
            "com.phonepe.app" to "Finance", "net.one97.paytm" to "Finance",
            "com.google.android.apps.nbu.paisa.user" to "Finance", // GPay
            "com.sbi.lotusintouch" to "Finance", // Yono SBI
            "com.zerodha.kite3" to "Finance",

            // Food & Drink
            "com.application.zomato" to "Food & Drink", "in.swiggy.android" to "Food & Drink"
        )
    }

    suspend fun categorizeApps(apps: List<AppUsageData>): List<AppUsageData> = withContext(Dispatchers.IO) {
        return@withContext apps.map { app ->
            val category = categorizeApp(app.packageName, app.appName)
            app.copy(category = category)
        }
    }

    private fun categorizeApp(packageName: String, appName: String): String {
        // 1. Check predefined categories map (most reliable)
        PREDEFINED_CATEGORIES[packageName]?.let {
            categoryCache[packageName] = it
            return it
        }

        // 2. Check cache
        categoryCache[packageName]?.let {
            return it
        }

        // 3. Use offline fallback logic
        val fallbackCategory = getFallbackCategory(appName, packageName)
        categoryCache[packageName] = fallbackCategory
        return fallbackCategory
    }

    private fun getFallbackCategory(appName: String, packageName: String): String {
        val lowerAppName = appName.lowercase()
        val lowerPackageName = packageName.lowercase()

        return when {
            lowerPackageName.contains("game") || lowerAppName.contains("game") -> "Games"
            lowerPackageName.contains("music") || lowerPackageName.contains("spotify") || lowerAppName.contains("audio") -> "Music"
            lowerPackageName.contains("camera") || lowerPackageName.contains("gallery") || lowerAppName.contains("photo") -> "Photography"
            lowerPackageName.contains("social") || lowerPackageName.contains("facebook") || lowerPackageName.contains("insta") -> "Social Media"
            lowerPackageName.contains("news") || lowerPackageName.contains("read") || lowerAppName.contains("news") -> "News & Reading"
            lowerPackageName.contains("shop") || lowerPackageName.contains("cart") || lowerAppName.contains("shop") -> "Shopping"
            lowerPackageName.contains("fitness") || lowerPackageName.contains("health") -> "Health & Fitness"
            lowerPackageName.contains("maps") || lowerPackageName.contains("navigation") || lowerAppName.contains("maps") -> "Navigation"
            lowerPackageName.contains("video") || lowerPackageName.contains("movie") || lowerPackageName.contains("youtube") -> "Entertainment"
            lowerPackageName.contains("chat") || lowerPackageName.contains("message") || lowerAppName.contains("chat") -> "Communication"
            lowerPackageName.contains("bank") || lowerPackageName.contains("pay") || lowerPackageName.contains("finance") -> "Finance"
            lowerPackageName.contains("food") || lowerPackageName.contains("zomato") || lowerPackageName.contains("swiggy") -> "Food & Drink"
            lowerPackageName.contains("browser") || lowerAppName.contains("browser") -> "Productivity"
            else -> "Utilities"
        }
    }

    fun clearCache() {
        categoryCache.clear()
    }
}