package com.lohith.scrollsense.util

import android.content.Context
import android.content.pm.PackageManager

object PackageNameHelper {
    fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val pm: PackageManager = context.packageManager
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
