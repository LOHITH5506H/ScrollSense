package com.lohith.scrollsense.util

import android.content.Context
import org.json.JSONObject

/**
 * Loads large keyword dictionaries from res/raw at runtime, without inflating the APK code size.
 *
 * Supported file names per language:
 *  - raw/keywords_en.json, raw/keywords_hi.json, raw/keywords_te.json, raw/keywords_es.json
 *
 * Each file is a simple JSON object mapping category id -> [keywords]. Example:
 * {
 *   "adult": ["nsfw", "xxx", "18+", "onlyfans"],
 *   "social": ["comment", "follow", "timeline"],
 *   ...
 * }
 */
object KeywordLoader {
    private val supportedLanguages = listOf("en", "hi", "te", "es")

    /**
     * Returns a nested map: category -> (language -> keywords)
     */
    fun loadAll(context: Context): Map<String, Map<String, List<String>>> {
        val result = mutableMapOf<String, MutableMap<String, List<String>>>()
        for (lang in supportedLanguages) {
            val resName = "keywords_$lang"
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            if (resId == 0) continue
            runCatching {
                context.resources.openRawResource(resId).use { input ->
                    val json = input.bufferedReader().readText()
                    val obj = JSONObject(json)
                    for (key in obj.keys()) {
                        val arr = obj.getJSONArray(key)
                        val list = buildList(arr.length()) {
                            for (i in 0 until arr.length()) add(arr.getString(i))
                        }
                        val byLang = result.getOrPut(key) { mutableMapOf() }
                        byLang[lang] = list
                    }
                }
            }
        }
        return result
    }
}
