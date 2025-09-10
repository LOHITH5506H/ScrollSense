// Enhanced CategoryClassifier.kt - PURELY CONTENT-AWARE VERSION
// Location: app/src/main/java/com/lohith/scrollsense/util/CategoryClassifier.kt

package com.lohith.scrollsense.util

class CategoryClassifier {

    companion object {

        // Extensive multilingual category keywords
        private val categories = mapOf(
            "adult" to mapOf(
                "en" to listOf(
                    "adult", "nsfw", "xxx", "porn", "erotic", "sex", "nude", "18+", "camgirl", "onlyfans",
                    "hentai", "explicit", "sensual", "lust", "fetish", "kink", "boudoir", "playboy",
                    "penthouse", "redtube", "pornhub", "xvideos", "youporn", "xhamster", "lewd"
                ),
                "hi" to listOf("वयस्क", "अश्लील", "सेक्स", "कामुक", "नग्न", "18+"),
                "te" to listOf("వయోజన", "అశ్లీల", "సెక్స్", "ఎరోటిక్", "న్యూడ్", "18+"),
                "es" to listOf("adulto", "nsfw", "xxx", "porno", "erótico", "sexo", "desnudo", "18+")
            ),
            "games" to mapOf(
                "en" to listOf(
                    "game", "gaming", "play", "level", "mission", "battle", "match", "multiplayer", "ranked",
                    "fps", "rpg", "strategy", "puzzle", "quest", "character", "upgrade", "loot", "boss",
                    "arena", "clan", "guild", "moba", "esports", "victory", "defeat", "respawn", "gameplay"
                ),
                "hi" to listOf("खेल", "गेम", "गेमिंग", "स्तर", "मिशन", "लड़ाई", "मैच"),
                "te" to listOf("ఆట", "గేమ్", "గేమింగ్", "స్థాయి", "మిషన్", "యుద్ధం", "మ్యాచ్"),
                "es" to listOf("juego", "juegos", "gaming", "nivel", "misión", "batalla", "partida")
            ),
            "social" to mapOf(
                "en" to listOf(
                    "social", "chat", "message", "comment", "post", "share", "like", "follow", "friend",
                    "family", "community", "group", "network", "profile", "status", "update", "notification",
                    "mention", "tag", "story", "dm", "pm", "tweet", "retweet", "feed", "timeline"
                ),
                "hi" to listOf("सामाजिक", "चैट", "संदेश", "टिप्पणी", "पोस्ट", "साझा करें", "लाइक"),
                "te" to listOf("సామాజిక", "చాట్", "సందేశం", "కామెంట్", "పోస్ట్", "షేర్", "లైక్"),
                "es" to listOf("social", "chat", "mensaje", "comentario", "publicación", "compartir")
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
            // "Other" category has no keywords; it's the default if no matches are found.
            "other" to mapOf(
                "en" to listOf(), "hi" to listOf(), "te" to listOf(), "es" to listOf()
            )
        )

        /**
         * This function now classifies content based ONLY on the text found on the screen.
         * The package name is ignored for categorization to ensure accuracy based on content.
         */
        fun classifyContent(text: String, packageName: String): String {
            val lowerText = text.lowercase()
            val scores = mutableMapOf<String, Int>()

            // Score each category based on keyword matches with weighting
            for ((category, languages) in categories) {
                var score = 0
                for ((_, keywords) in languages) {
                    for (keyword in keywords) {
                        if (lowerText.contains(keyword.lowercase())) {
                            // Longer, more specific keywords get a higher score
                            score += when (keyword.length) {
                                in 1..3 -> 1
                                in 4..6 -> 2
                                else -> 3
                            }
                        }
                    }
                }
                if (score > 0) {
                    scores[category] = score
                }
            }

            // Return the category with the highest score, or "other" if no keywords matched.
            return scores.maxByOrNull { it.value }?.key ?: "other"
        }

        fun detectLanguage(text: String): String {
            return when {
                text.any { it in '\u0900'..'\u097F' } -> "hi" // Devanagari
                text.any { it in '\u0C00'..'\u0C7F' } -> "te" // Telugu
                text.any { it in 'À'..'ÿ' } -> "es" // Latin extended
                else -> "en"
            }
        }

        fun getDetailedClassification(text: String, packageName: String): ClassificationResult {
            val primaryCategory = classifyContent(text, packageName)
            val language = detectLanguage(text)

            return ClassificationResult(
                category = primaryCategory,
                language = language,
                confidence = 1.0f, // Confidence is 1.0 as it's a direct keyword match
                keywords = emptyList()
            )
        }

        fun guessGameName(packageName: String, text: String): String? {
            val lowerPkg = packageName.lowercase()
            val lowerText = text.lowercase()
            val known = listOf(
                "freefire" to "Free Fire", "garena" to "Free Fire", "pubg" to "PUBG/BGMI",
                "bgmi" to "BGMI", "codm" to "Call of Duty", "callofduty" to "Call of Duty",
                "asphalt" to "Asphalt", "candycrush" to "Candy Crush", "genshin" to "Genshin Impact",
                "roblox" to "Roblox", "minecraft" to "Minecraft", "pokemon" to "Pokémon",
                "clashofclans" to "Clash of Clans", "clashroyale" to "Clash Royale", "fortnite" to "Fortnite"
            )
            known.firstOrNull { (k, _) -> lowerPkg.contains(k) || lowerText.contains(k) }?.let { return it.second }
            return null
        }
    }

    data class ClassificationResult(
        val category: String,
        val language: String,
        val confidence: Float,
        val keywords: List<String>
    )
}