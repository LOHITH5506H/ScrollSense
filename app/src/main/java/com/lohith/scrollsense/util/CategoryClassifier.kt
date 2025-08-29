// Enhanced CategoryClassifier.kt with Multilingual Support
// Location: app/src/main/java/com/lohith/scrollsense/util/CategoryClassifier.kt

package com.lohith.scrollsense.util

import android.content.Context
import org.json.JSONObject
import java.io.IOException

class CategoryClassifier {

    companion object {

        // Multilingual category keywords
        private val categories = mapOf(
            "education" to mapOf(
                "en" to listOf(
                    "learn", "study", "education", "course", "tutorial", "lesson", "class",
                    "university", "college", "school", "academic", "research", "knowledge",
                    "training", "workshop", "lecture", "exam", "test", "homework", "assignment"
                ),
                "hi" to listOf(
                    "सीखना", "अध्ययन", "शिक्षा", "कोर्स", "ट्यूटोरियल", "पाठ", "कक्षा",
                    "विश्वविद्यालय", "कॉलेज", "स्कूल", "शैक्षणिक", "अनुसंधान", "ज्ञान",
                    "प्रशिक्षण", "कार्यशाला", "व्याख्यान", "परीक्षा", "गृहकार्य"
                ),
                "te" to listOf(
                    "నేర్చుకోవడం", "అధ్యయనం", "విద్య", "కోర్సు", "ట్యుటోరియల్", "పాఠం", "తరగతి",
                    "విశ్వవిద్యాలయం", "కళాశాల", "పాఠశాల", "విద్యా", "పరిశోధన", "జ్ఞానం",
                    "శిక్షణ", "వర్క్‌షాప్", "ఉపన్యాసం", "పరీక్ష", "హోంవర్క్"
                ),
                "es" to listOf(
                    "aprender", "estudiar", "educación", "curso", "tutorial", "lección", "clase",
                    "universidad", "colegio", "escuela", "académico", "investigación", "conocimiento",
                    "entrenamiento", "taller", "conferencia", "examen", "tarea"
                )
            ),

            "entertainment" to mapOf(
                "en" to listOf(
                    "movie", "film", "video", "music", "game", "fun", "entertainment", "comedy",
                    "drama", "action", "horror", "romance", "adventure", "documentary", "series",
                    "show", "episode", "season", "actor", "director", "soundtrack", "trailer"
                ),
                "hi" to listOf(
                    "फिल्म", "वीडियो", "संगीत", "गेम", "मनोरंजन", "कॉमेडी", "ड्रामा",
                    "एक्शन", "हॉरर", "रोमांस", "एडवेंचर", "डॉक्यूमेंट्री", "सीरियल",
                    "शो", "एपिसोड", "सीजन", "अभिनेता", "निर्देशक", "ट्रेलर"
                ),
                "te" to listOf(
                    "సినిమా", "వీడియో", "సంగీతం", "ఆట", "వినోదం", "కామెడీ", "డ్రామా",
                    "యాక్షన్", "హార్రర్", "రొమాన్స్", "అడ్వెంచర్", "డాక్యుమెంటరీ", "సీరియల్",
                    "షో", "ఎపిసోడ్", "సీజన్", "నటుడు", "దర్శకుడు", "ట్రైలర్"
                ),
                "es" to listOf(
                    "película", "video", "música", "juego", "diversión", "entretenimiento", "comedia",
                    "drama", "acción", "terror", "romance", "aventura", "documental", "serie",
                    "programa", "episodio", "temporada", "actor", "director", "trailer"
                )
            ),

            "technology" to mapOf(
                "en" to listOf(
                    "technology", "tech", "programming", "code", "software", "hardware", "computer",
                    "android", "ios", "app", "application", "development", "developer", "API",
                    "database", "algorithm", "artificial intelligence", "machine learning", "AI", "ML"
                ),
                "hi" to listOf(
                    "तकनीक", "प्रोग्रामिंग", "कोड", "सॉफ्टवेयर", "हार्डवेयर", "कंप्यूटर",
                    "एंड्रॉइड", "आईओएस", "ऐप", "एप्लिकेशन", "डेवलपमेंट", "डेवलपर",
                    "डेटाबेस", "एल्गोरिदम", "आर्टिफिशियल इंटेलिजेंस", "मशीन लर्निंग"
                ),
                "te" to listOf(
                    "టెక్నాలజీ", "ప్రోగ్రామింగ్", "కోడ్", "సాఫ్ట్‌వేర్", "హార్డ్‌వేర్", "కంప్యూటర్",
                    "ఆండ్రాయిడ్", "ఐఓఎస్", "యాప్", "అప్లికేషన్", "డెవలప్‌మెంట్", "డెవలపర్",
                    "డేటాబేస్", "అల్గోరిథం", "కృత్రిమ మేథస్", "మెషిన్ లెర్నింగ్"
                ),
                "es" to listOf(
                    "tecnología", "programación", "código", "software", "hardware", "computadora",
                    "android", "ios", "aplicación", "desarrollo", "desarrollador", "API",
                    "base de datos", "algoritmo", "inteligencia artificial", "aprendizaje automático"
                )
            ),

            "music" to mapOf(
                "en" to listOf(
                    "music", "song", "album", "artist", "singer", "musician", "band", "concert",
                    "playlist", "track", "audio", "sound", "melody", "rhythm", "lyrics", "genre",
                    "rock", "pop", "jazz", "classical", "hip hop", "electronic", "folk"
                ),
                "hi" to listOf(
                    "संगीत", "गाना", "एल्बम", "कलाकार", "गायक", "संगीतकार", "बैंड", "कंसर्ट",
                    "प्लेलिस्ट", "ट्रैक", "ऑडियो", "आवाज", "धुन", "ताल", "बोल", "शैली"
                ),
                "te" to listOf(
                    "సంగీతం", "పాట", "ఆల్బమ్", "కళాకారుడు", "గాయకుడు", "సంగీత విద్వాంసుడు", "బ్యాండ్", "కాన్సర్ట్",
                    "ప్లేలిస్ట్", "ట్రాక్", "ఆడియో", "శబ్దం", "రాగం", "తాళం", "సాహిత్యం", "శైలి"
                ),
                "es" to listOf(
                    "música", "canción", "álbum", "artista", "cantante", "músico", "banda", "concierto",
                    "lista de reproducción", "pista", "audio", "sonido", "melodía", "ritmo", "letra", "género"
                )
            ),

            "sports" to mapOf(
                "en" to listOf(
                    "sports", "football", "basketball", "cricket", "tennis", "soccer", "baseball",
                    "hockey", "golf", "swimming", "running", "fitness", "gym", "exercise", "workout",
                    "athlete", "competition", "tournament", "match", "game", "score", "team", "player"
                ),
                "hi" to listOf(
                    "खेल", "फुटबॉल", "बास्केटबॉल", "क्रिकेट", "टेनिस", "सॉकर", "बेसबॉल",
                    "हॉकी", "गोल्फ", "तैराकी", "दौड़", "फिटनेस", "जिम", "व्यायाम", "वर्कआउट",
                    "एथलीट", "प्रतियोगिता", "टूर्नामेंट", "मैच", "खेल", "स्कोर", "टीम", "खिलाड़ी"
                ),
                "te" to listOf(
                    "క్రీడలు", "ఫుట్‌బాల్", "బాస్కెట్‌బాల్", "క్రికెట్", "టెన్నిస్", "సాకర్", "బేస్‌బాల్",
                    "హాకీ", "గోల్ఫ్", "స్విమ్మింగ్", "రన్నింగ్", "ఫిట్‌నెస్", "జిమ్", "వ్యాయామం", "వర్కౌట్",
                    "అథ్లెట్", "పోటీ", "టోర్నమెంట్", "మ్యాచ్", "ఆట", "స్కోర్", "టీమ్", "ప్లేయర్"
                ),
                "es" to listOf(
                    "deportes", "fútbol", "baloncesto", "cricket", "tenis", "soccer", "béisbol",
                    "hockey", "golf", "natación", "correr", "fitness", "gimnasio", "ejercicio", "entrenamiento",
                    "atleta", "competencia", "torneo", "partido", "juego", "puntaje", "equipo", "jugador"
                )
            ),

            "news" to mapOf(
                "en" to listOf(
                    "news", "breaking", "politics", "government", "election", "policy", "economy",
                    "market", "business", "finance", "world", "international", "national", "local",
                    "reporter", "journalist", "headline", "article", "press", "media", "current events"
                ),
                "hi" to listOf(
                    "समाचार", "ब्रेकिंग", "राजनीति", "सरकार", "चुनाव", "नीति", "अर्थव्यवस्था",
                    "बाजार", "व्यापार", "वित्त", "विश्व", "अंतर्राष्ट्रीय", "राष्ट्रीय", "स्थानीय",
                    "रिपोर्टर", "पत्रकार", "सुर्खी", "लेख", "प्रेस", "मीडिया", "वर्तमान घटनाएं"
                ),
                "te" to listOf(
                    "వార్తలు", "బ్రేకింగ్", "రాజకీయాలు", "ప్రభుత్వం", "ఎన్నికలు", "విధానం", "ఆర్థిక వ్యవస్థ",
                    "మార్కెట్", "వ్యాపారం", "ఫైనాన్స్", "ప్రపంచం", "అంతర్జాతీయ", "జాతీయ", "స్థానిక",
                    "రిపోర్టర్", "జర్నలిస్ట్", "హెడ్‌లైన్", "వ్యాసం", "ప్రెస్", "మీడియా", "ప్రస్తుత సంఘటనలు"
                ),
                "es" to listOf(
                    "noticias", "última hora", "política", "gobierno", "elección", "política", "economía",
                    "mercado", "negocio", "finanzas", "mundo", "internacional", "nacional", "local",
                    "reportero", "periodista", "titular", "artículo", "prensa", "medios", "eventos actuales"
                )
            ),

            "food" to mapOf(
                "en" to listOf(
                    "food", "recipe", "cooking", "cuisine", "restaurant", "meal", "breakfast", "lunch",
                    "dinner", "snack", "ingredient", "chef", "kitchen", "baking", "healthy", "diet",
                    "nutrition", "vegetarian", "vegan", "dessert", "appetizer", "main course"
                ),
                "hi" to listOf(
                    "खाना", "रेसिपी", "खाना पकाना", "व्यंजन", "रेस्टोरेंट", "भोजन", "नाश्ता", "दोपहर का खाना",
                    "रात का खाना", "स्नैक", "सामग्री", "शेफ", "रसोई", "बेकिंग", "स्वस्थ", "आहार",
                    "पोषण", "शाकाहारी", "वीगन", "मिठाई", "एपेटाइज़र", "मुख्य व्यंजन"
                ),
                "te" to listOf(
                    "ఆహారం", "రెసిపీ", "వంట", "వంటకాలు", "రెస్టారెంట్", "భోజనం", "అల్పాహారం", "మధ్యాహ్న భోజనం",
                    "రాత్రి భోజనం", "స్నాక్", "పదార్థం", "చెఫ్", "వంటగది", "బేకింగ్", "ఆరోగ్యకరమైన", "ఆహారం",
                    "పోషణ", "శాకాహారి", "వేగన్", "డెజర్ట్", "అపెటైజర్", "ప్రధాన వంటకం"
                ),
                "es" to listOf(
                    "comida", "receta", "cocinar", "cocina", "restaurante", "comida", "desayuno", "almuerzo",
                    "cena", "snack", "ingrediente", "chef", "cocina", "hornear", "saludable", "dieta",
                    "nutrición", "vegetariano", "vegano", "postre", "aperitivo", "plato principal"
                )
            ),

            "business" to mapOf(
                "en" to listOf(
                    "business", "finance", "investment", "stock", "market", "economy", "company", "startup",
                    "entrepreneur", "marketing", "sales", "profit", "revenue", "budget", "strategy",
                    "management", "leadership", "corporate", "industry", "commerce", "trade"
                ),
                "hi" to listOf(
                    "व्यापार", "वित्त", "निवेश", "स्टॉक", "बाजार", "अर्थव्यवस्था", "कंपनी", "स्टार्टअप",
                    "उद्यमी", "मार्केटिंग", "बिक्री", "लाभ", "राजस्व", "बजट", "रणनीति",
                    "प्रबंधन", "नेतृत्व", "कॉर्पोरेट", "उद्योग", "वाणिज्य", "व्यापार"
                ),
                "te" to listOf(
                    "వ్యాపారం", "ఫైనాన్స్", "పెట్టుబడి", "స్టాక్", "మార్కెట్", "ఆర్థిక వ్యవస్థ", "కంపెనీ", "స్టార్టప్",
                    "వ్యాపారవేత్త", "మార్కెటింగ్", "అమ్మకాలు", "లాభం", "ఆదాయం", "బడ్జెట్", "వ్యూహం",
                    "నిర్వహణ", "నాయకత్వం", "కార్పొరేట్", "పరిశ్రమ", "వాణిజ్యం", "వ్యాపారం"
                ),
                "es" to listOf(
                    "negocio", "finanzas", "inversión", "acción", "mercado", "economía", "empresa", "startup",
                    "emprendedor", "marketing", "ventas", "ganancia", "ingresos", "presupuesto", "estrategia",
                    "gestión", "liderazgo", "corporativo", "industria", "comercio", "comercio"
                )
            ),

            "social" to mapOf(
                "en" to listOf(
                    "social", "chat", "message", "comment", "post", "share", "like", "follow",
                    "friend", "family", "relationship", "community", "group", "network", "profile",
                    "status", "update", "notification", "mention", "tag", "story"
                ),
                "hi" to listOf(
                    "सामाजिक", "चैट", "संदेश", "टिप्पणी", "पोस्ट", "साझा", "लाइक", "फॉलो",
                    "दोस्त", "परिवार", "रिश्ता", "समुदाय", "समूह", "नेटवर्क", "प्रोफ़ाइल",
                    "स्थिति", "अपडेट", "अधिसूचना", "उल्लेख", "टैग", "कहानी"
                ),
                "te" to listOf(
                    "సామాజిక", "చాట్", "సందేశం", "కామెంట్", "పోస్ట్", "షేర్", "లైక్", "ఫాలో",
                    "స్నేహితుడు", "కుటుంబం", "సంబంధం", "కమ్యూనిటీ", "గ్రూప్", "నెట్‌వర్క్", "ప్రొఫైల్",
                    "స్థితి", "అప్‌డేట్", "నోటిఫికేషన్", "మెన్షన్", "ట్యాగ్", "కథ"
                ),
                "es" to listOf(
                    "social", "chat", "mensaje", "comentario", "publicación", "compartir", "me gusta", "seguir",
                    "amigo", "familia", "relación", "comunidad", "grupo", "red", "perfil",
                    "estado", "actualización", "notificación", "mención", "etiqueta", "historia"
                )
            ),

            "other" to mapOf(
                "en" to listOf("other", "miscellaneous", "general", "unknown", "various"),
                "hi" to listOf("अन्य", "विविध", "सामान्य", "अज्ञात", "विभिन्न"),
                "te" to listOf("ఇతర", "వివిధ", "సాధారణ", "తెలియని", "వైవిధ్యమైన"),
                "es" to listOf("otro", "misceláneo", "general", "desconocido", "varios")
            )
        )

        // Package name to category mapping for better classification
        private val packageCategories = mapOf(
            "youtube" to "entertainment",
            "netflix" to "entertainment",
            "spotify" to "music",
            "instagram" to "social",
            "whatsapp" to "social",
            "facebook" to "social",
            "twitter" to "social",
            "chrome" to "technology",
            "gmail" to "business",
            "maps" to "navigation",
            "camera" to "photography",
            "gallery" to "photography",
            "calculator" to "productivity",
            "calendar" to "productivity",
            "clock" to "productivity",
            "weather" to "lifestyle",
            "news" to "news",
            "shopping" to "business",
            "banking" to "business",
            "fitness" to "sports",
            "health" to "sports"
        )

        fun classifyContent(text: String, packageName: String): String {
            // First try package-based classification
            val packageCategory = classifyByPackage(packageName)
            if (packageCategory != "other") {
                return packageCategory
            }

            // Then try content-based classification
            return classifyByContent(text)
        }

        private fun classifyByPackage(packageName: String): String {
            val lowerPackage = packageName.lowercase()

            for ((keyword, category) in packageCategories) {
                if (lowerPackage.contains(keyword)) {
                    return category
                }
            }

            return "other"
        }

        private fun classifyByContent(text: String): String {
            val lowerText = text.lowercase()
            val scores = mutableMapOf<String, Int>()

            // Score each category based on keyword matches
            for ((category, languages) in categories) {
                var score = 0

                for ((_, keywords) in languages) {
                    for (keyword in keywords) {
                        if (lowerText.contains(keyword.lowercase())) {
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

            // Return category with highest score
            return scores.maxByOrNull { it.value }?.key ?: "other"
        }

        fun getAllCategories(): List<String> {
            return categories.keys.toList()
        }

        fun getCategoryKeywords(category: String, language: String = "en"): List<String> {
            return categories[category]?.get(language) ?: emptyList()
        }

        fun detectLanguage(text: String): String {
            // Simple language detection based on character sets
            return when {
                text.any { it in '\u0900'..'\u097F' } -> "hi" // Devanagari script
                text.any { it in '\u0C00'..'\u0C7F' } -> "te" // Telugu script
                text.any { it in 'À'..'ÿ' } -> "es" // Latin extended (Spanish)
                else -> "en" // Default to English
            }
        }

        fun getDetailedClassification(text: String, packageName: String): ClassificationResult {
            val primaryCategory = classifyContent(text, packageName)
            val language = detectLanguage(text)
            val confidence = calculateConfidence(text, primaryCategory)

            return ClassificationResult(
                category = primaryCategory,
                language = language,
                confidence = confidence,
                keywords = getCategoryKeywords(primaryCategory, language)
            )
        }

        private fun calculateConfidence(text: String, category: String): Float {
            val lowerText = text.lowercase()
            val categoryKeywords = categories[category] ?: return 0.0f

            var matches = 0
            var totalKeywords = 0

            for ((_, keywords) in categoryKeywords) {
                totalKeywords += keywords.size
                matches += keywords.count { lowerText.contains(it.lowercase()) }
            }

            return if (totalKeywords > 0) matches.toFloat() / totalKeywords else 0.0f
        }
    }

    data class ClassificationResult(
        val category: String,
        val language: String,
        val confidence: Float,
        val keywords: List<String>
    )
}