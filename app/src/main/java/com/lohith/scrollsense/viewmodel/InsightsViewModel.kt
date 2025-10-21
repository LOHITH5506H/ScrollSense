package com.lohith.scrollsense.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.lohith.scrollsense.BuildConfig
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.DailyAppAnalytics
import com.lohith.scrollsense.data.DailyCategoryAnalytics
import com.lohith.scrollsense.data.DailySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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
        val weekCats: List<DailyCategoryAnalytics> = dailyDao.getCategoryAnalyticsRange(weekStartStr, endStr)
        val monthCats: List<DailyCategoryAnalytics> = dailyDao.getCategoryAnalyticsRange(monthStartStr, endStr)
        val weekApps: List<DailyAppAnalytics> = dailyDao.getAppAnalyticsRange(weekStartStr, endStr)
        val monthApps: List<DailyAppAnalytics> = dailyDao.getAppAnalyticsRange(monthStartStr, endStr)

        fun totals(summaries: List<DailySummary>) = summaries.sumOf { it.totalScreenTimeMs }

        return UsageSnapshot(
            today = dailySummary,
            weekTotal = totals(weekSummaries),
            monthTotal = totals(monthSummaries),
            topWeekCategory = weekCats.groupBy { it.category }.mapValues { it.value.sumOf { d -> d.totalTimeMs } }.maxByOrNull { it.value }?.key,
            topMonthCategory = monthCats.groupBy { it.category }.mapValues { it.value.sumOf { d -> d.totalTimeMs } }.maxByOrNull { it.value }?.key,
            topWeekApp = weekApps.groupBy { it.appName }.mapValues { it.value.sumOf { d -> d.totalTimeMs } }.maxByOrNull { it.value }?.key,
            topMonthApp = monthApps.groupBy { it.appName }.mapValues { it.value.sumOf { d -> d.totalTimeMs } }.maxByOrNull { it.value }?.key,
            weekDays = weekSummaries.size,
            monthDays = monthSummaries.size
        )
    }

    private suspend fun callGeminiOrFallback(snapshot: UsageSnapshot): String {
        val key = BuildConfig.GEMINI_API_KEY
        val hasKey = key.isNotBlank() && key != "pasteKeyHere"
        val prompt = buildPrompt(snapshot)
        return if (hasKey) {
            try {
                val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = key)
                val resp = model.generateContent(prompt)
                resp.text?.takeIf { it.isNotBlank() } ?: fallbackInsight(snapshot)
            } catch (_: Throwable) {
                fallbackInsight(snapshot)
            }
        } else {
            fallbackInsight(snapshot)
        }
    }

    private fun buildPrompt(s: UsageSnapshot): String {
        val todayPart = if (s.today != null) {
            "Today: total=${formatDuration(s.today.totalScreenTimeMs)}, topCategory=${s.today.topCategory}, topApp=${s.today.topApp}."
        } else "Today: no data."
        val weekAvg = if (s.weekDays > 0) formatDuration(s.weekTotal / s.weekDays) else "0m"
        val monthAvg = if (s.monthDays > 0) formatDuration(s.monthTotal / s.monthDays) else "0m"
        return """
            You are a helpful digital wellness coach. Analyze this phone usage:
            - $todayPart
            - Last 7 days: total=${formatDuration(s.weekTotal)}, avgPerDay=$weekAvg, topCategory=${s.topWeekCategory ?: "-"}, topApp=${s.topWeekApp ?: "-"}.
            - Last 30 days: total=${formatDuration(s.monthTotal)}, avgPerDay=$monthAvg, topCategory=${s.topMonthCategory ?: "-"}, topApp=${s.topMonthApp ?: "-"}.

            Provide 3 concise, specific insights and 3 actionable suggestions to improve balance. Keep it under 120 words. Use friendly, encouraging tone. No lists longer than 6 items.
        """.trimIndent()
    }

    private fun fallbackInsight(s: UsageSnapshot): String {
        val today = s.today
        val builder = StringBuilder()
        if (today != null) {
            builder.append("Today you spent ${formatDuration(today.totalScreenTimeMs)}. ")
            builder.append("Top category: ${today.topCategory}. Top app: ${today.topApp}. ")
        } else builder.append("No data for today. ")
        if (s.weekDays > 0) builder.append("Weekly average: ${formatDuration(s.weekTotal / s.weekDays)}. ")
        if (s.monthDays > 0) builder.append("Monthly average: ${formatDuration(s.monthTotal / s.monthDays)}. ")
        builder.append("Try a 20-min focus block and schedule a short walk. Mute non-urgent notifications in your top category during work hours.")
        return builder.toString()
    }

    private fun formatDuration(ms: Long): String {
        val mins = ms / 60000
        val h = mins / 60
        val m = mins % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    // Helpers for dates without java.time (minSdk 24 compatible)
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
        val topWeekCategory: String?,
        val topMonthCategory: String?,
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
