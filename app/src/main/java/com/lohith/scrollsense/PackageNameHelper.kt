package com.lohith.scrollsense

import android.content.Context
import android.content.pm.PackageManager

object PackageNameHelper {
    fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
