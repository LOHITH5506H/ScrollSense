package com.lohith.scrollsense.util

import android.util.Log

object CategoryClassifier {
    private const val TAG = "CategoryClassifier"

    // MUCH MORE SPECIFIC KEYWORDS TO PREVENT MISCLASSIFICATION
    private val categories = mapOf(

        // Programming - VERY STRICT KEYWORDS ONLY
        "programming" to listOf(
            "coding tutorial", "programming tutorial", "learn to code", "how to code",
            "kotlin tutorial", "java tutorial", "python tutorial", "javascript tutorial",
            "android development", "web development", "app development", "software development",
            "coding course", "programming course", "coding bootcamp", "coding interview",
            "data structures", "algorithms tutorial", "programming basics", "learn programming",
            "coding for beginners", "programming for beginners", "developer tutorial",
            "coding challenge", "programming challenge", "code review", "debugging tutorial"
        ),

        // Music - CLEAR MUSIC INDICATORS
        "music" to listOf(
            "official music video", "music video", "official video", "lyric video",
            "live performance", "concert", "acoustic version", "cover song", "remix",
            "new song", "hit song", "album", "single", "band", "singer", "artist",
            "music review", "song lyrics", "music reaction", "live concert", "unplugged",
            "music album", "soundtrack", "musical", "opera", "symphony", "jazz music",
            "rock music", "pop music", "hip hop music", "electronic music", "classical music"
        ),

        // Sports - CLEAR SPORTS CONTENT
        "sports" to listOf(
            "football match", "soccer match", "cricket match", "basketball game",
            "tennis match", "sports highlights", "match highlights", "game highlights",
            "fifa world cup", "nba finals", "champions league", "premier league",
            "olympics", "world championship", "tournament final", "sports news",
            "player interview", "match analysis", "sports commentary", "live sports",
            "football skills", "soccer skills", "sports training", "workout routine"
        ),

        // Entertainment - MOVIES, TV, COMEDY
        "entertainment" to listOf(
            "movie trailer", "film trailer", "movie review", "film review", "comedy show",
            "stand up comedy", "funny video", "comedy sketch", "tv show", "series",
            "netflix series", "movie clips", "film analysis", "celebrity interview",
            "talk show", "late night show", "comedy special", "movie reaction",
            "film reaction", "behind the scenes", "bloopers", "funny moments",
            "viral video", "meme compilation", "entertainment news", "red carpet"
        ),

        // Education - LEARNING CONTENT (NOT PROGRAMMING)
        "education" to listOf(
            "history lesson", "science lesson", "math tutorial", "physics tutorial",
            "chemistry tutorial", "biology tutorial", "geography lesson", "language learning",
            "educational video", "documentary", "how things work", "explained",
            "crash course", "khan academy", "ted talk", "lecture", "university course",
            "online course", "study guide", "exam preparation", "homework help",
            "learning english", "math problems", "science experiment", "historical facts"
        ),

        // News - NEWS AND CURRENT EVENTS
        "news" to listOf(
            "breaking news", "news update", "current events", "news analysis",
            "political news", "election news", "government news", "world news",
            "local news", "business news", "economic news", "market news",
            "news report", "news interview", "press conference", "news commentary",
            "political debate", "election coverage", "news summary", "daily news"
        ),

        // Health & Fitness - HEALTH CONTENT
        "health" to listOf(
            "workout routine", "fitness training", "gym workout", "home workout",
            "yoga class", "meditation guide", "health tips", "nutrition guide",
            "diet plan", "healthy recipes", "weight loss", "muscle building",
            "cardio workout", "strength training", "mental health", "wellness tips",
            "healthy lifestyle", "fitness motivation", "exercise routine", "health advice"
        ),

        // Food - COOKING AND FOOD
        "food" to listOf(
            "cooking tutorial", "recipe video", "how to cook", "cooking show",
            "food review", "restaurant review", "cooking tips", "baking tutorial",
            "food preparation", "cooking techniques", "kitchen tips", "chef tutorial",
            "homemade recipe", "easy recipes", "cooking channel", "food tasting",
            "cooking competition", "masterchef", "food challenge", "cooking skills"
        ),

        // Travel - TRAVEL AND PLACES
        "travel" to listOf(
            "travel vlog", "travel guide", "vacation video", "trip video",
            "travel tips", "travel review", "destination guide", "city tour",
            "travel documentary", "adventure travel", "backpacking", "road trip",
            "travel experience", "places to visit", "travel advice", "hotel review",
            "travel budget", "solo travel", "family vacation", "travel planning"
        ),

        // Art & Design - VISUAL ARTS
        "art" to listOf(
            "art tutorial", "drawing tutorial", "painting tutorial", "art lesson",
            "digital art", "art process", "speed painting", "art review",
            "gallery tour", "art exhibition", "artist interview", "art history",
            "art techniques", "art supplies", "art tips", "creative process",
            "illustration", "graphic design", "art challenge", "art inspiration"
        ),

        // Gaming - VIDEO GAMES
        "gaming" to listOf(
            "gameplay", "game review", "gaming tutorial", "game walkthrough",
            "gaming news", "game trailer", "gaming setup", "game analysis",
            "gaming highlights", "game commentary", "gaming tips", "game guide",
            "gaming channel", "esports", "gaming competition", "game stream",
            "gaming reaction", "game discussion", "gaming community", "game update"
        )
    )

    fun classify(text: String): String {
        if (text.isBlank()) return "other"

        val lowerText = text.lowercase().trim()

        // DEBUG LOG
        Log.d(TAG, "🎯 Classifying: '$text'")

        // Calculate scores for each category with STRICT MATCHING
        val scores = mutableMapOf<String, Int>()

        for ((category, keywords) in categories) {
            var score = 0
            val matchedKeywords = mutableListOf<String>()

            for (keyword in keywords) {
                when {
                    // Exact phrase match - highest score
                    lowerText.contains(keyword) -> {
                        score += keyword.length * 3
                        matchedKeywords.add(keyword)
                    }
                    // All words in keyword present - medium score
                    keyword.split(" ").size > 1 &&
                            keyword.split(" ").all { word -> lowerText.contains(word) } -> {
                        score += keyword.length
                        matchedKeywords.add("partial: $keyword")
                    }
                }
            }

            if (score > 0) {
                scores[category] = score
                Log.d(TAG, "   $category: score=$score, matches=$matchedKeywords")
            }
        }

        // Only return category if score is high enough (STRICT THRESHOLD)
        val bestMatch = scores.maxByOrNull { it.value }
        val result = if (bestMatch != null && bestMatch.value >= 10) {
            bestMatch.key
        } else {
            "other"
        }

        Log.d(TAG, "   RESULT: '$result' (best score: ${bestMatch?.value ?: 0})")
        return result
    }

    fun getAllCategories(): List<String> {
        return categories.keys.toList()
    }

    fun getKeywords(category: String): List<String> {
        return categories[category] ?: emptyList()
    }

    // Method to test classification
    fun testClassification(text: String): Map<String, Int> {
        val lowerText = text.lowercase()
        val scores = mutableMapOf<String, Int>()

        for ((category, keywords) in categories) {
            var score = 0
            for (keyword in keywords) {
                if (lowerText.contains(keyword)) {
                    score += keyword.length * 3
                }
            }
            if (score > 0) {
                scores[category] = score
            }
        }

        return scores
    }
}
