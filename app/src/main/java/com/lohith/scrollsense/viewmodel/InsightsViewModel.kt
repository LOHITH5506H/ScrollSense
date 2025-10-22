package com.lohith.scrollsense.viewmodel

import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.lohith.scrollsense.BuildConfig
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.DailySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx = application
    private val dailyDao = AppDatabase.getDatabase(application).dailySummaryDao()

    private val _insightText = MutableStateFlow("")
    val insightText: StateFlow<String> = _insightText

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _lastGeneratedEpochDay = MutableStateFlow<Long>(-1)
    val lastGeneratedEpochDay: StateFlow<Long> = _lastGeneratedEpochDay

    init {
        loadFromPrefs()
    }

    fun canGenerateNow(): Boolean {
        val today = currentEpochDay()
        return _lastGeneratedEpochDay.value < today
//        return true
    }

    fun timeUntilNextWindowMillis(): Long {
        val tz = TimeZone.getDefault()
        val nowUtc = System.currentTimeMillis()
        val nowLocal = nowUtc + tz.getOffset(nowUtc)
        val nextLocalMidnight = ((nowLocal / MILLIS_PER_DAY) + 1) * MILLIS_PER_DAY
        val deltaLocal = nextLocalMidnight - nowLocal
        return if (deltaLocal > 0) deltaLocal else 0L
    }

    private fun loadFromPrefs() {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _insightText.value = prefs.getString(KEY_TEXT, "").orEmpty()
        _lastGeneratedEpochDay.value = prefs.getLong(KEY_EPOCH_DAY, -1L)
    }

    private fun saveToPrefs(text: String) {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TEXT, text)
            .putLong(KEY_EPOCH_DAY, currentEpochDay())
            .apply()
        _insightText.value = text
        _lastGeneratedEpochDay.value = currentEpochDay()
    }

    fun generateInsight() {
        if (!canGenerateNow()) return
        _isGenerating.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { buildSnapshot() }
                val text = withContext(Dispatchers.IO) { callGeminiOrFallback(snapshot) }
                saveToPrefs(text)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate insight"
                Log.e("InsightsViewModel", "Error generating insight", e)
                saveToPrefs(fallbackInsight())
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun buildSnapshot(): UsageSnapshot {
        val todayStr = formatDateString(daysAgo = 0)
        val weekStartStr = formatDateString(daysAgo = 6)
        val monthStartStr = formatDateString(daysAgo = 29)
        val endStr = todayStr

        val dailySummary: DailySummary? = dailyDao.getDailySummary(todayStr)
        val weekSummaries: List<DailySummary> = dailyDao.getDailySummaries(weekStartStr, endStr)
        val monthSummaries: List<DailySummary> = dailyDao.getDailySummaries(monthStartStr, endStr)

        val weekCats = dailyDao.getCategoryAnalyticsRange(weekStartStr, endStr)
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.totalTimeMs } }

        val monthCats = dailyDao.getCategoryAnalyticsRange(monthStartStr, endStr)
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.totalTimeMs } }

        val weekApps = dailyDao.getAppAnalyticsRange(weekStartStr, endStr)
            .groupBy { it.appName }
            .mapValues { entry -> entry.value.sumOf { it.totalTimeMs } }

        val monthApps = dailyDao.getAppAnalyticsRange(monthStartStr, endStr)
            .groupBy { it.appName }
            .mapValues { entry -> entry.value.sumOf { it.totalTimeMs } }

        return UsageSnapshot(
            today = dailySummary,
            weekTotal = weekSummaries.sumOf { it.totalScreenTimeMs },
            monthTotal = monthSummaries.sumOf { it.totalScreenTimeMs },
            topWeekCategories = weekCats.entries.sortedByDescending { it.value }.take(4).associate { it.toPair() },
            topMonthCategories = monthCats.entries.sortedByDescending { it.value }.take(4).associate { it.toPair() },
            topWeekApp = weekApps.maxByOrNull { it.value }?.key,
            topMonthApp = monthApps.maxByOrNull { it.value }?.key,
            weekDays = weekSummaries.size,
            monthDays = monthSummaries.size
        )
    }

    private suspend fun callGeminiOrFallback(snapshot: UsageSnapshot): String {
        val key = BuildConfig.GEMINI_API_KEY
        val hasKey = key.isNotBlank() && key != "pasteKeyHere" && key != "RANDOM"

        if (!hasKey) {
            Log.w("GeminiAI", "API key is missing. Using fallback.")
            return fallbackInsight()
        }

        val prompt = buildPrompt(snapshot)
        Log.d("GeminiAI", "Prompt: $prompt")

        val modelName = "gemini-2.5-flash-lite"
        var lastSdkError: Throwable? = null

        try {
            Log.d("GeminiAI", "Attempting SDK with model: $modelName")
            val model = GenerativeModel(
                modelName = modelName,
                apiKey = key,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    topK = 40
                    topP = 0.95f
                    maxOutputTokens = 200
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
                )
            )
            val response = model.generateContent(prompt)
            val text = response.text
            if (!text.isNullOrBlank()) {
                Log.d("GeminiAI", "SDK call successful with $modelName.")
                return text
            }
        } catch (e: Throwable) {
            lastSdkError = e
            Log.w("GeminiAI", "SDK call with $modelName failed: ${e.message}")
        }

        Log.e("GeminiAI", "SDK attempt failed. Trying direct REST API call as fallback.", lastSdkError)

        try {
            val restResult = tryGeminiRest(prompt, key)
            if (!restResult.isNullOrBlank()) {
                return restResult
            }
        } catch (e: Throwable) {
            Log.e("GeminiAI", "Direct REST call also failed.", e)
        }

        return fallbackInsight()
    }

    private suspend fun tryGeminiRest(prompt: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash-lite:generateContent?key=$apiKey"
        val client = OkHttpClient()
        val jsonBody = """
        {
          "contents": [{
            "parts":[{
              "text": ${JSONObject.quote(prompt)}
            }]
          }]
        }
        """.trimIndent()

        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e("GeminiAI_REST", "Request failed: ${response.code} - $body")
                    return@withContext null
                }
                val text = JSONObject(body)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                Log.d("GeminiAI_REST", "Direct REST call successful.")
                return@withContext text
            }
        } catch (e: Exception) {
            Log.e("GeminiAI_REST", "Exception during REST call", e)
            return@withContext null
        }
    }

    private fun buildPrompt(s: UsageSnapshot): String {
        val todayPart = s.today?.let { "Today's total usage was ${formatDuration(it.totalScreenTimeMs)}, with most time on ${it.topApp}." } ?: "No usage data for today."
        val weekAvg = if (s.weekDays > 0) formatDuration(s.weekTotal / s.weekDays) else "0m"

        // Filter out "other" and take the top 3 meaningful categories
        val meaningfulCategories = s.topWeekCategories
            .filterKeys { it.lowercase() != "other" }
            .toList()
            .take(3)

        val topCategoriesText = meaningfulCategories.joinToString(", ") { "${it.first} (${formatDuration(it.second)})" }

        val promptCategoryLine = if (topCategoriesText.isNotBlank()) {
            "- Top categories this week: $topCategoriesText"
        } else {
            "- No specific app categories stood out this week."
        }

        return """
            You are a helpful and encouraging digital wellness coach.
            Analyze the following phone usage data for a user:
            - Today's Summary: $todayPart
            - 7-Day Average Daily Usage: $weekAvg
            $promptCategoryLine

            Based on this data, provide a brief, actionable insight in a maximum of 2 friendly sentences.
            Your goal is to be practical and encouraging, not judgmental.
            IMPORTANT: Start each sentence on a new line, and do not use a numbered or bulleted list.
        """.trimIndent()
    }

    private fun fallbackInsight(): String {
        val insights = listOf(
            "\nReady for a mini-challenge?\nTry putting your phone away for the first 15 minutes after you wake up.",
            "\nDesignate a 'phone-free' zone in your home, like the dinner table.\nThis helps encourage more presence with others.",
            "\nBefore unlocking your phone, take a breath and ask: 'What am I opening this for?'\nIt's a great way to reduce mindless scrolling.",
            "\nFeeling cluttered? Take 5 minutes to delete an old app you no longer use.\nA tidy phone can lead to a tidy mind.",
            "\nNext time you feel the urge to scroll, try a 5-minute stretch instead.\nYour body and mind will thank you for the break!",
            "\nBuilding new habits takes time, and you're on the right track.\nKeep up the great work on being mindful of your usage!"
        )
        return insights[Random.nextInt(insights.size)]
    }

    private fun formatDuration(ms: Long): String {
        if (ms < 60000) return "<1m"
        val mins = ms / 60000
        val h = mins / 60
        val m = mins % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private fun formatDateString(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        if (daysAgo != 0) cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }

    private fun currentEpochDay(): Long {
        val tz = TimeZone.getDefault()
        val nowUtc = System.currentTimeMillis()
        val local = nowUtc + tz.getOffset(nowUtc)
        return local / MILLIS_PER_DAY
    }

    data class UsageSnapshot(
        val today: DailySummary?,
        val weekTotal: Long,
        val monthTotal: Long,
        val topWeekCategories: Map<String, Long>,
        val topMonthCategories: Map<String, Long>,
        val topWeekApp: String?,
        val topMonthApp: String?,
        val weekDays: Int,
        val monthDays: Int
    )

    companion object {
        private const val PREFS_NAME = "ai_insights"
        private const val KEY_TEXT = "last_text"
        private const val KEY_EPOCH_DAY = "last_epoch_day"
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}