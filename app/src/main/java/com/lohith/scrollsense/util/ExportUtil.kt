package com.lohith.scrollsense.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.lohith.scrollsense.data.UsageEvent
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Utility helpers for exporting / sharing usage data and reading category metadata. */
object ExportUtil {
    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun usageEventsToCsv(events: List<UsageEvent>): String {
        val header = listOf(
            "id","package","appLabel","screenTitle","category","subcategory","language","confidence","startTime","endTime","durationMs","startReadable","endReadable"
        )
        val sb = StringBuilder()
        sb.append(header.joinToString(","))
        events.forEach { e ->
            sb.append('\n')
            val row = listOf(
                e.id,
                esc(e.packageName),
                esc(e.appLabel),
                esc(e.screenTitle),
                esc(e.category),
                esc(e.subcategory),
                esc(e.language),
                e.confidence,
                e.startTime,
                e.endTime,
                e.durationMs,
                esc(dateTimeFmt.format(Date(e.startTime))),
                esc(dateTimeFmt.format(Date(e.endTime)))
            )
            sb.append(row.joinToString(","))
        }
        return sb.toString()
    }

    /** Share the events as a text/csv payload using a chooser. */
    fun shareUsageEvents(context: Context, events: List<UsageEvent>) {
        if (events.isEmpty()) return
        val csv = usageEventsToCsv(events)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "ScrollSense Usage Data Export")
            putExtra(Intent.EXTRA_TEXT, csv)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Export Usage Data").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (t: Throwable) {
            Log.e("ExportUtil", "Failed to share usage data", t)
        }
    }

    /** Load multilingual category labels from raw/categories.json if present. */
    fun loadCategoryLabels(context: Context, language: String = Locale.getDefault().language): Map<String, String> =
        runCatching {
            val resId = context.resources.getIdentifier("categories", "raw", context.packageName)
            if (resId == 0) return emptyMap()
            context.resources.openRawResource(resId).use { input ->
                val txt = BufferedReader(InputStreamReader(input)).readText()
                val root = JSONObject(txt)
                val arr = root.getJSONArray("categories")
                buildMap {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.getString("id")
                        val label = obj.optString(language, obj.optString("en", id))
                        put(id, label)
                    }
                }
            }
        }.getOrElse { emptyMap() }

    private fun esc(v: Any?): String = buildString {
        val s = v?.toString() ?: ""
        if (s.any { it == ',' || it == '"' || it == '\n' }) {
            append('"')
            append(s.replace("\"", "\"\""))
            append('"')
        } else append(s)
    }
}
