package com.lohith.scrollsense.util

import android.content.Context
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.workers.RetentionWorker // Import is still needed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DataWiper {
    suspend fun wipeAppStorage(context: Context) {
        withContext(Dispatchers.IO) {
            // 1. Clear the database
            val db = AppDatabase.getDatabase(context)
            db.clearAllTables()
            db.close()

            // This line is correctly commented out
            // PreferencesManager(context).clearAll()

            // 3. Clear cached files (this is safe)
            deleteRecursive(context.cacheDir)

            // ------------------------------------------------------------------
            // FIX: Removed the call to RetentionWorker.cancelAll(context)
            // as the function does not exist.
            // ------------------------------------------------------------------
        }
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { deleteRecursive(it) }
        }
        fileOrDirectory.delete()
    }
}