package com.lohith.scrollsense.util

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.lohith.scrollsense.BuildConfig

object GeminiClassifier {

    private val generativeModel by lazy {
        try {
            if (BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "null") {
                GenerativeModel(
                    // --- THIS IS THE CORRECT, NEW MODEL NAME ---
                    modelName = "gemini-1.5-flash-latest",
                    apiKey = BuildConfig.GEMINI_API_KEY
                )
            } else {
                Log.e("GeminiClassifier", "API Key is missing. Check local.properties.")
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiClassifier", "Error initializing GenerativeModel.", e)
            null
        }
    }

    private val categoryList = listOf(
        "Gaming", "Education", "Technology", "Sports", "Music", "News",
        "Business", "Entertainment", "Shopping", "Social Media", "Communication",
        "Lifestyle", "Food", "Finance", "Productivity", "Health & Fitness", "Other"
    )
    private val categorySet = categoryList.toSet()

    suspend fun classify(appName: String, contentTitle: String): String {
        val model = generativeModel ?: return "Other"
        if (contentTitle.isBlank()) return "Other"

        return try {
            val prompt = """
                You are an expert content classifier. Classify the following app usage into ONE of these specific categories: ${categoryList.joinToString(", ")}.
                Respond with only the single, most appropriate category name.

                --- EXAMPLES ---
                App Name: "YouTube", Screen Title: "Learn Kotlin in 6 Hours" -> Education
                App Name: "Instagram", Screen Title: "Instagram" -> Social Media
                App Name: "Clash of Clans", Screen Title: "Village View" -> Gaming
                App Name: "The Economic Times", Screen Title: "Market Rises Sharply" -> News
                App Name: "WhatsApp", Screen Title: "John Doe" -> Communication
                --- END EXAMPLES ---

                Now, classify this new entry:
                App Name: "$appName"
                Screen Title: "$contentTitle"
            """.trimIndent()

            val response = model.generateContent(prompt)
            val category = response.text?.trim()?.split("\n")?.firstOrNull()?.trim()

            category?.let { cat ->
                if (cat in categorySet) {
                    cat
                } else {
                    Log.w("GeminiClassifier", "Gemini returned an invalid category: '$cat'. Falling back.")
                    "Other"
                }
            } ?: "Other"

        } catch (e: Exception) {
            Log.e("GeminiClassifier", "Error during Gemini classification: ${e.message}")
            "Other"
        }
    }
}