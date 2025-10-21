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
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AiInsightsViewModelCompat(application: Application) : AndroidViewModel(application) {
    private val ctx = application
    private val dailyDao = AppDatabase.getDatabase(application).dailySummaryDao()

    private val _insightText = MutableStateFlow("")
    val insightText: StateFlow<String> = _insightText

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _lastGeneratedEpochDay = MutableStateFlow(-1L)
    val lastGeneratedEpochDay: StateFlow<Long> = _lastGeneratedEpochDay

    init { loadFromPrefs() }

    fun canGenerateNow(): Boolean {
        return _lastGeneratedEpochDay.value < currentEpochDay()
    }

    fun timeUntilNextWindowMillis(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return (cal.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0)
    }

    private fun currentEpochDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return TimeUnit.MILLISECONDS.toDays(cal.timeInMillis)
    }

    private fun loadFromPrefs() {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _insightText.value = p.getString(KEY_TEXT, "").orEmpty()
        _lastGeneratedEpochDay.value = p.getLong(KEY_DAY, -1L)
    }

    private fun saveToPrefs(text: String) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val day = currentEpochDay()
        p.edit().putString(KEY_TEXT, text).putLong(KEY_DAY, day).apply()
        _insightText.value = text
        _lastGeneratedEpochDay.value = day
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

    private suspend fun buildSnapshot(): Snapshot {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Date()
        fun minusDays(d: Int): Date = Calendar.getInstance().apply { time = today; add(Calendar.DAY_OF_YEAR, -d) }.time
        val todayS = fmt.format(today)
        val weekS = fmt.format(minusDays(6))
        val monthS = fmt.format(minusDays(29))

        val daily: DailySummary? = dailyDao.getDailySummary(todayS)
        val weekSummaries = dailyDao.getDailySummaries(weekS, todayS)
        val monthSummaries = dailyDao.getDailySummaries(monthS, todayS)
        val weekCats = dailyDao.getCategoryAnalyticsRange(weekS, todayS)
        val monthCats = dailyDao.getCategoryAnalyticsRange(monthS, todayS)
        val weekApps = dailyDao.getAppAnalyticsRange(weekS, todayS)
        val monthApps = dailyDao.getAppAnalyticsRange(monthS, todayS)

        fun total(list: List<DailySummary>) = list.sumOf { it.totalScreenTimeMs }
        fun topName(groups: Map<String, Long>) = groups.maxByOrNull { it.value }?.key

        return Snapshot(
            daily,
            total(weekSummaries),
            total(monthSummaries),
            topName(weekCats.groupBy { it.category }.mapValues { it.value.sumOf { r -> r.totalTimeMs } }),
            topName(monthCats.groupBy { it.category }.mapValues { it.value.sumOf { r -> r.totalTimeMs } }),
            topName(weekApps.groupBy { it.appName }.mapValues { it.value.sumOf { r -> r.totalTimeMs } }),
            topName(monthApps.groupBy { it.appName }.mapValues { it.value.sumOf { r -> r.totalTimeMs } }),
            weekSummaries.size,
            monthSummaries.size
        )
    }

    private suspend fun callGeminiOrFallback(s: Snapshot): String {
        val key = BuildConfig.GEMINI_API_KEY
        val prompt = buildPrompt(s)
        val canCall = key.isNotBlank() && key != "pasteKeyHere"
        if (!canCall) return fallback(s)
        return try {
            val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = key)
            val resp = model.generateContent(prompt)
            resp.text?.takeIf { it.isNotBlank() } ?: fallback(s)
        } catch (_: Throwable) {
            fallback(s)
        }
    }

    private fun buildPrompt(s: Snapshot): String {
        val today = s.daily?.let { "Today: ${fmt(it.totalScreenTimeMs)}; top cat ${it.topCategory}; top app ${it.topApp}." } ?: "Today: no data."
        val weekAvg = if (s.weekDays > 0) fmt(s.weekTotal / s.weekDays) else "0m"
        val monthAvg = if (s.monthDays > 0) fmt(s.monthTotal / s.monthDays) else "0m"
        return """
            You are a helpful digital wellness coach. Analyze this usage:
            - $today
            - Last 7 days: total=${fmt(s.weekTotal)}, avgPerDay=$weekAvg, topCategory=${s.topWeekCategory ?: "-"}, topApp=${s.topWeekApp ?: "-"}.
            - Last 30 days: total=${fmt(s.monthTotal)}, avgPerDay=$monthAvg, topCategory=${s.topMonthCategory ?: "-"}, topApp=${s.topMonthApp ?: "-"}.

            Provide 3 short insights and 3 actionable tips (max ~120 words, friendly tone).
        """.trimIndent()
    }

    private fun fallback(s: Snapshot): String {
        val today = s.daily?.let { "Today ${fmt(it.totalScreenTimeMs)}; top: ${it.topCategory}/${it.topApp}. " } ?: "No data today. "
        val week = if (s.weekDays > 0) "Weekly avg ${fmt(s.weekTotal / s.weekDays)}. " else ""
        val month = if (s.monthDays > 0) "Monthly avg ${fmt(s.monthTotal / s.monthDays)}. " else ""
        return today + week + month + "Try a 20-min focus block, a short walk, and mute non-urgent notifications in your top category during work hours."
    }

    private fun fmt(ms: Long): String {
        val m = ms / 60000
        val h = m / 60
        val mm = m % 60
        return if (h > 0) "${h}h ${mm}m" else "${mm}m"
    }

    data class Snapshot(
        val daily: DailySummary?,
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
        private const val PREFS = "ai_insights"
        private const val KEY_TEXT = "last_text"
        private const val KEY_DAY = "last_epoch_day"
    }
}

