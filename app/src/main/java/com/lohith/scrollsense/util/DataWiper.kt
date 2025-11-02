package com.lohith.scrollsense.util

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Utility to wipe application data from internal storage,
 * excluding the 'lib' directory.
 */
object DataWiper {
    private const val TAG = "DataWiper"

    /**
     * Wipes app storage (cache, shared_prefs, files, databases).
     * This is a destructive operation.
     */
    fun wipeAppStorage(context: Context) {
        try {
            val cacheDir: File = context.cacheDir
            val appDir: File = File(cacheDir.parent)
            if (appDir.exists()) {
                val children: Array<String> = appDir.list() ?: return
                for (s in children) {
                    // Don't delete the native libraries folder
                    if (s != "lib") {
                        val f = File(appDir, s)
                        if (deleteDir(f)) {
                            Log.i(TAG, "Successfully deleted $f")
                        } else {
                            Log.e(TAG, "Failed to delete $f")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wipe app storage", e)
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir == null) return true
        if (dir.isDirectory) {
            val children: Array<String> = dir.list() ?: return false
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        // The directory is now empty or it's a file
        return dir.delete()
    }
}