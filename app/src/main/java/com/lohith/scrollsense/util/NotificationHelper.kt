package com.lohith.scrollsense.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lohith.scrollsense.R // Make sure this R is your app's R

object NotificationHelper {

    private const val LIMIT_CHANNEL_ID = "app_limit_channel"

    fun sendLimitNotification(
        context: Context,
        appName: String,
        packageName: String,
        limitMinutes: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Limit Alerts"
            val descriptionText = "Notifications for when app time limits are exceeded"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(LIMIT_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Time Limit Reached"
        val text = "You've used $appName for over $limitMinutes minutes today."

        // You MUST have this icon in your res/drawable folder
        // I'm using the monochrome launcher icon you already have
        val icon = R.drawable.ic_launcher_monochrome

        val builder = NotificationCompat.Builder(context, LIMIT_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Use a unique ID for each app's notification so they don't overwrite each other
        val notificationId = packageName.hashCode()
        notificationManager.notify(notificationId, builder.build())
    }
}