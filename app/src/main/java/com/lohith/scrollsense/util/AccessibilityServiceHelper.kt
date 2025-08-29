package com.lohith.scrollsense.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.lohith.scrollsense.MyAccessibilityService

object AccessibilityServiceHelper {
    fun isServiceEnabled(context: Context): Boolean {
        val fqcn = MyAccessibilityService::class.qualifiedName ?: return false
        val expectedFull = "${context.packageName}/$fqcn"
        val expectedShort = "${context.packageName}/.${fqcn.substringAfterLast('.')}" // typical flattened form
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val next = colonSplitter.next()
            if (next.equals(expectedFull, ignoreCase = true) ||
                next.equals(expectedShort, ignoreCase = true) ||
                (next.startsWith(context.packageName) && next.contains(fqcn))
            ) return true
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
