package com.lohith.scrollsense.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Parcelize
@Entity(tableName = "app_usage")
data class AppUsageData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    var category: String = "Utilities",
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val launchCount: Int,
    val dateRecorded: String = getCurrentDateString(),
    val isSystemApp: Boolean = false,
    val appIconUri: String? = null
) : Parcelable {

    fun getFormattedTime(): String {
        val hours = totalTimeInForeground / 3600000
        val minutes = (totalTimeInForeground % 3600000) / 60000

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${totalTimeInForeground / 1000}s"
        }
    }

    fun getUsagePercentage(totalUsage: Long): Double {
        if (totalUsage == 0L) return 0.0
        return ((totalTimeInForeground.toDouble() / totalUsage) * 100)
    }

    fun getFormattedLastUsed(): String {
        return if (lastTimeUsed > 0) {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            sdf.format(Date(lastTimeUsed))
        } else {
            "Never"
        }
    }

    fun getUsageHours(): Float {
        return (totalTimeInForeground / 3600000f)
    }

    fun getUsageMinutes(): Float {
        return (totalTimeInForeground / 60000f)
    }

    fun isSignificantUsage(): Boolean {
        return totalTimeInForeground >= 60000 // At least 1 minute
    }

    companion object {
        fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}