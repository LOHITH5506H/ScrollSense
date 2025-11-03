// In a new file, e.g., data/models/AppInfo.kt
package com.lohith.scrollsense.data.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)