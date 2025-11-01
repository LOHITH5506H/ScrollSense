package com.lohith.scrollsense.util

import android.content.Context
import java.io.File

object DataWiper {
    fun wipeAppStorage(context: Context) {
        deleteRecursively(context.cacheDir)
        deleteRecursively(context.filesDir)
        // Keep SharedPreferences except our parental ones; caller can clear preferences if needed
    }

    private fun deleteRecursively(file: File?) {
        if (file == null || !file.exists()) return
        if (file.isDirectory) file.listFiles()?.forEach { deleteRecursively(it) }
        runCatching { file.delete() }
    }
}
