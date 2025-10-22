// Enhanced CategoryClassifier.kt - HIERARCHICAL, HEURISTIC & EXTERNALIZED VETO VERSION
// Location: app/src/main/java/com/lohith/scrollsense/util/CategoryClassifier.kt

package com.lohith.scrollsense.util

import android.content.Context
import java.util.regex.Pattern

object CategoryClassifier {

    // --- NEW: This will be loaded from res/raw/veto_keywords_en.json ---
    @Volatile private var loadedVetoKeywords: Set<String> = emptySet()

    // --- LAYER 2: UI NOISE FILTER ---
    private val UI_NOISE_WORDS = setOf(
        "share", "like", "comment", "view", "reply", "settings", "search", "filter",
        "results", "menu", "home", "about", "contact", "privacy", "terms", "help",
        "feedback", "ago", "translate", "google", "account", "explore", "add", "amp"
    )

    // (The baseCategories map remains the same as before)
    private val baseCategories = mapOf(
        "adult" to mapOf(
            "en" to listOf(
                "adult", "nsfw", "xxx", "erotic", "18+"
            ),
            "hi" to listOf("वयस्क", "अश्लील", "18+"),
            "te" to listOf("వయోజన", "అశ్లీల", "18+"),
            "es" to listOf("adulto", "xxx", "18+")
        ),
        "games" to mapOf(
            "en" to listOf(
                "game", "gaming", "play", "level", "mission", "battle", "match", "multiplayer", "ranked",
                "fps", "rpg", "strategy", "puzzle", "quest", "character", "upgrade", "loot", "boss",
                "arena", "clan", "guild", "moba", "esports", "victory", "defeat", "respawn", "gameplay"
            ),
            "hi" to listOf("खेल", "गेम", "गेमिंग", "स्तर", "మిషన్", "लड़ाई", "मैच"),
            "te" to listOf("ఆట", "గేమ్", "గేమింగ్", "స్థాయి", "మిషన్", "యుద్ధం", "మ్యాచ్"),
            "es" to listOf("juego", "juegos", "gaming", "nivel", "misión", "batalla", "partida")
        ),
        "social" to mapOf(
            "en" to listOf(
                "social", "chat", "message", "post", "follow", "friend",
                "family", "community", "group", "network", "profile", "status", "update", "notification",
                "mention", "tag", "story", "dm", "pm", "tweet", "retweet", "feed", "timeline",
                "call", "dialling", "video call", "voice call", "incallui"
            ),
            "hi" to listOf("सामाजिक", "चैट", "संदेश", "टिप्पणी", "पोस्ट", "साझा करें", "लाइक", "कॉल"),
            "te" to listOf("సామాజిక", "చాట్", "సందేశం", "కామెంట్", "పోస్ట్", "షేర్", "లైక్", "కాల్"),
            "es" to listOf("social", "chat", "mensaje", "comentario", "publicación", "compartir", "llamada")
        ),
        "entertainment" to mapOf(
            "en" to listOf(
                "movie", "film", "video", "fun", "entertainment", "comedy", "drama", "action", "horror",
                "romance", "adventure", "documentary", "series", "show", "episode", "season", "actor",
                "director", "trailer", "tv", "binge", "watch", "stream", "netflix", "prime video", "hotstar"
            ),
            "hi" to listOf("फिल्म", "वीडियो", "मनोरंजन", "कॉमेडी", "ड्रामा", "सीरियल", "शो"),
            "te" to listOf("సినిమా", "వీడియో", "వినోదం", "కామెడీ", "డ్రామా", "సీరియల్", "షో"),
            "es" to listOf("película", "video", "entretenimiento", "comedia", "drama", "serie", "programa")
        ),
        "news" to mapOf(
            "en" to listOf(
                "news", "breaking", "politics", "government", "election", "economy", "world", "international",
                "national", "local", "reporter", "journalist", "headline", "article", "press", "media", "report",
                "daily", "times", "express", "chronicle", "tribune", "gazette", "journal"
            ),
            "hi" to listOf("समाचार", "ब्रेकिंग", "राजनीति", "सरकार", "चुनाव", "अर्थव्यवस्था", "रिपोर्टर", "पत्रकार"),
            "te" to listOf("వార్తలు", "బ్రేకింగ్", "రాజకీయాలు", "ప్రభుత్వం", "ఎన్నికలు", "ఆర్థిక వ్యవస్థ", "రిపోర్టర్"),
            "es" to listOf("noticias", "última hora", "política", "gobierno", "elección", "economía", "periodista")
        ),
        "technology" to mapOf(
            "en" to listOf(
                "technology", "tech", "programming", "code", "software", "hardware", "computer", "android",
                "ios", "app", "development", "developer", "api", "database", "algorithm", "ai", "ml", "crypto",
                "blockchain", "gadget", "review", "unboxing", "specs", "cpu", "gpu", "ram", "storage"
            ),
            "hi" to listOf("तकनीक", "प्रोग्रामिंग", "कोड", "सॉफ्टवेयर", "हार्डवेयर", "कंप्यूटर", "ऐप"),
            "te" to listOf("టెక్నాలజీ", "ప్రోగ్రామింగ్", "కోడ్", "సాఫ్ట్‌వేర్", "హార్డ్‌వేర్", "కంప్యూటర్"),
            "es" to listOf("tecnología", "programación", "código", "software", "hardware", "computadora")
        ),
        "education" to mapOf(
            "en" to listOf(
                "learn", "study", "education", "course", "tutorial", "lesson", "class", "university", "college",
                "school", "academic", "research", "knowledge", "training", "lecture", "exam", "test", "homework",
                "assignment", "science", "history", "math", "geography", "physics", "chemistry", "biology"
            ),
            "hi" to listOf("सीखना", "अध्ययन", "शिक्षा", "कोर्स", "ट्यूटोरियल", "पाठ", "कक्षा", "ज्ञान"),
            "te" to listOf("నేర్చుకోవడం", "అధ్యయనం", "విద్య", "కోర్సు", "ట్యుటోరియల్", "పాఠం", "జ్ఞానం"),
            "es" to listOf("aprender", "estudiar", "educación", "curso", "tutorial", "lección", "clase")
        ),
        "business" to mapOf(
            "en" to listOf(
                "business", "finance", "investment", "stock", "market", "economy", "company", "startup",
                "entrepreneur", "marketing", "sales", "profit", "revenue", "budget", "strategy", "management",
                "corporate", "industry", "commerce", "trade", "linkedin", "stocks", "ipo", "mutual fund",
                "hedge fund", "data warehousing", "data mining"
            ),
            "hi" to listOf("व्यापार", "वित्त", "निवेश", "स्टॉक", "बाजार", "अर्थव्यवस्था", "कंपनी"),
            "te" to listOf("వ్యాపారం", "ఫైనాన్స్", "పెట్టుబడి", "స్టాక్", "మార్కెట్", "ఆర్థిక వ్యవస్థ"),
            "es" to listOf("negocio", "finanzas", "inversión", "mercado", "economía", "empresa")
        ),
        "fitness" to mapOf(
            "en" to listOf(
                "fitness", "gym", "workout", "exercise", "health", "wellness", "nutrition", "protein", "cardio",
                "yoga", "running", "lifting", "weights", "muscle", "abs", "bodybuilding", "trainer"
            ),
            "hi" to listOf("फिटनेस", "जिम", "वर्कआउट", "व्यायाम", "स्वास्थ्य", "वेलनेस", "पोषण"),
            "te" to listOf("ఫిట్‌నెస్", "జిమ్", "వర్కౌట్", "వ్యాయామం", "ఆరోగ్యం", "సంరక్షణ"),
            "es" to listOf("fitness", "gimnasio", "entrenamiento", "ejercicio", "salud", "bienestar")
        ),
        "other" to mapOf(
            "en" to listOf(), "hi" to listOf(), "te" to listOf(), "es" to listOf()
        )
    )

    @Volatile private var externalKeywords: Map<String, Map<String, List<String>>> = emptyMap()

    fun init(context: Context) {
        externalKeywords = KeywordLoader.loadAll(context)
        loadedVetoKeywords = loadVetoKeywords(context)
    }

    private fun loadVetoKeywords(context: Context): Set<String> {
        return runCatching {
            val resId = context.resources.getIdentifier("veto_keywords_en", "raw", context.packageName)
            if (resId == 0) return emptySet()
            context.resources.openRawResource(resId).use { input ->
                val json = input.bufferedReader().readText()
                val obj = org.json.JSONObject(json)
                val arr = obj.getJSONArray("adult_veto")
                val keywords = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    keywords.add(arr.getString(i).lowercase())
                }
                keywords
            }
        }.getOrDefault(emptySet())
    }

    private fun mergedCategories(): Map<String, Map<String, List<String>>> {
        val keys = (baseCategories.keys + externalKeywords.keys).toSet()
        val out = mutableMapOf<String, Map<String, List<String>>>()
        for (cat in keys) {
            val base = baseCategories[cat] ?: emptyMap()
            val ext = externalKeywords[cat] ?: emptyMap()
            val langs = (base.keys + ext.keys).toSet()
            val langMap = mutableMapOf<String, List<String>>()
            for (lang in langs) {
                val baseList = base[lang] ?: emptyList()
                val extList = ext[lang] ?: emptyList()
                val merged = (extList + baseList).distinct()
                langMap[lang] = merged
            }
            out[cat] = langMap
        }
        if (!out.containsKey("adult")) {
            out["adult"] = baseCategories["adult"] ?: emptyMap()
        }
        if (!out.containsKey("fitness")) {
            out["fitness"] = baseCategories["fitness"] ?: emptyMap()
        }
        return out
    }

    /**
     * Classifies content using a 3-layer hierarchical algorithm.
     * 1. Veto Layer: Immediately classifies "adult" content and stops.
     * 2. Noise-Cancelling Scorer: Removes UI words, then scores the remaining text.
     * 3. Heuristic Layer: Uses package name as a fallback for ambiguous content.
     */
    fun classifyContent(text: String, packageName: String): String {
        val lowerText = text.lowercase()

        // --- LAYER 1: VETO SYSTEM ---
        for (vetoWord in loadedVetoKeywords) {
            val pattern = "\\b${Pattern.quote(vetoWord)}\\b".toRegex(RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(lowerText)) {
                return "adult" // Immediate classification, process stops.
            }
        }

        // --- LAYER 2: NOISE-CANCELING SCORER ---
        val noisePattern = UI_NOISE_WORDS.joinToString(separator = "|") { "\\b${Pattern.quote(it)}\\b" }
        val cleanedText = lowerText.replace(noisePattern.toRegex(RegexOption.IGNORE_CASE), "")

        val categories = mergedCategories()
        val scores = mutableMapOf<String, Int>()

        for ((category, languages) in categories) {
            if (category == "adult") continue

            var categoryScore = 0
            for ((_, keywords) in languages) {
                for (keyword in keywords) {
                    if (keyword.isBlank()) continue

                    val pattern = "\\b${Pattern.quote(keyword)}\\b".toRegex(RegexOption.IGNORE_CASE)
                    val matches = pattern.findAll(cleanedText).count()

                    if (matches > 0) {
                        val weight = when (keyword.length) {
                            in 1..3 -> 1
                            in 4..7 -> 3
                            else -> 5
                        }
                        categoryScore += matches * weight
                    }
                }
            }
            if (categoryScore > 0) {
                scores[category] = categoryScore
            }
        }

        val topResult = scores.maxByOrNull { it.value }
        val topCategory = topResult?.key
        val topScore = topResult?.value ?: 0

        // --- LAYER 3: HEURISTIC SAFETY NET ---
        val MINIMUM_CONFIDENCE_SCORE = 5
        if (topScore < MINIMUM_CONFIDENCE_SCORE) {
            val lowerPackageName = packageName.lowercase()
            return when {
                lowerPackageName.contains("youtube") -> "entertainment"
                lowerPackageName.contains("netflix") -> "entertainment"
                lowerPackageName.contains("hotstar") -> "entertainment"
                lowerPackageName.contains("whatsapp") -> "social"
                lowerPackageName.contains("instagram") -> "social"
                lowerPackageName.contains("facebook") -> "social"
                lowerPackageName.contains("chrome") -> "productivity"
                lowerPackageName.contains("browser") -> "productivity"
                else -> topCategory ?: "other"
            }
        }

        return topCategory ?: "other"
    }
}