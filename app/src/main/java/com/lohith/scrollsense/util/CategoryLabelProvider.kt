package com.lohith.scrollsense.util

import android.content.Context
import org.json.JSONObject
import java.util.Locale

object CategoryLabelProvider {
    @Volatile private var cache: Map<String, Map<String, String>>? = null

    private fun load(context: Context): Map<String, Map<String, String>> {
        cache?.let { return it }
        val resId = context.resources.getIdentifier("categories", "raw", context.packageName)
        if (resId == 0) return emptyMap()
        val json = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("categories")
        val map = mutableMapOf<String, Map<String, String>>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val id = item.getString("id")
            val translations = mutableMapOf<String, String>()
            for (key in item.keys()) {
                if (key == "id") continue
                translations[key] = item.getString(key)
            }
            map[id] = translations
        }
        cache = map
        return map
    }

    fun getDisplayName(context: Context, categoryId: String, locale: Locale = Locale.getDefault()): String {
        val data = load(context)
        val id = categoryId.lowercase(Locale.getDefault()).trim()
        val translations = data[id] ?: return id.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        val lang = locale.language
        return translations[lang]
            ?: translations["en"]
            ?: id.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}

