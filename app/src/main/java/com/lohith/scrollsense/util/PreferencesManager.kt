package com.lohith.scrollsense.util

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesManager private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getRetentionDays(): Int = prefs.getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)

    fun setRetentionDays(days: Int) {
        prefs.edit().putInt(KEY_RETENTION_DAYS, days).apply()
    }

    fun isParentPasswordSet(): Boolean = prefs.contains(KEY_PARENT_PASSWORD)

    fun setParentPassword(plain: String) {
        val hash = sha256(plain)
        prefs.edit().putString(KEY_PARENT_PASSWORD, hash).apply()
    }

    fun verifyParentPassword(plain: String): Boolean {
        val stored = prefs.getString(KEY_PARENT_PASSWORD, null) ?: return false
        return stored == sha256(plain)
    }

    fun getParentalLimits(): Map<String, Int> {
        val json = prefs.getString(KEY_PARENT_LIMITS, "{}") ?: "{}"
        val obj = JSONObject(json)
        val map = mutableMapOf<String, Int>()
        obj.keys().forEach { pkg -> map[pkg] = obj.optInt(pkg, 0) }
        return map
    }

    fun setParentalLimit(packageName: String, minutesPerDay: Int) {
        val obj = JSONObject(prefs.getString(KEY_PARENT_LIMITS, "{}") ?: "{}")
        obj.put(packageName, minutesPerDay)
        prefs.edit().putString(KEY_PARENT_LIMITS, obj.toString()).apply()
    }

    fun removeParentalLimit(packageName: String) {
        val obj = JSONObject(prefs.getString(KEY_PARENT_LIMITS, "{}") ?: "{}")
        obj.remove(packageName)
        prefs.edit().putString(KEY_PARENT_LIMITS, obj.toString()).apply()
        // also clear alert flag
        val alerts = JSONObject(prefs.getString(KEY_PARENT_ALERTED, "{}") ?: "{}")
        alerts.remove(packageName)
        prefs.edit().putString(KEY_PARENT_ALERTED, alerts.toString()).apply()
    }

    fun isAlertedForToday(packageName: String, now: Long = System.currentTimeMillis()): Boolean {
        val today = dayKey(now)
        val alerts = JSONObject(prefs.getString(KEY_PARENT_ALERTED, "{}") ?: "{}")
        return alerts.optString(packageName, "") == today
    }

    fun markAlertedForToday(packageName: String, now: Long = System.currentTimeMillis()) {
        val today = dayKey(now)
        val alerts = JSONObject(prefs.getString(KEY_PARENT_ALERTED, "{}") ?: "{}")
        alerts.put(packageName, today)
        prefs.edit().putString(KEY_PARENT_ALERTED, alerts.toString()).apply()
    }

    companion object {
        private const val PREFS = "scrollsense_prefs"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_PARENT_PASSWORD = "parent_pwd_hash"
        private const val KEY_PARENT_LIMITS = "parent_limits" // JSON: { pkg: minutes }
        private const val KEY_PARENT_ALERTED = "parent_alerted" // JSON: { pkg: yyyyMMdd }
        const val DEFAULT_RETENTION_DAYS = 30

        @Volatile private var INSTANCE: PreferencesManager? = null
        fun get(context: Context): PreferencesManager = INSTANCE ?: synchronized(this){
            INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
        }

        private fun sha256(input: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray())
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        private fun dayKey(ms: Long): String {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            return sdf.format(Date(ms))
        }
    }
}
