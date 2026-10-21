package com.example.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.QuickCallApp
import com.example.data.model.AppUpdateInfo

/**
 * Handles sending Android system notifications when a new application release
 * or mandatory force-update is discovered.
 */
object UpdateNotificationHelper {

    const val NOTIFICATION_UPDATE_ID = 9001
    const val EXTRA_START_UPDATE = "extra_start_update"

    fun showUpdateNotification(
        context: Context,
        updateInfo: AppUpdateInfo,
        isForceUpdate: Boolean
    ) {
        if (!PermissionHelper.hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_UPDATE, true)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val title = if (isForceUpdate) {
            "🚨 Critical Update Required: Quick Call v${updateInfo.versionName}"
        } else {
            "⚡ New Update Available: Quick Call v${updateInfo.versionName}"
        }

        val message = if (isForceUpdate) {
            "Mandatory privacy & security update (v${updateInfo.versionName}). Please update now to continue using Quick Call Scan."
        } else {
            "Quick Call Scan v${updateInfo.versionName} is ready. Tap to install the latest features and bug fixes."
        }

        val builder = NotificationCompat.Builder(context, QuickCallApp.CHANNEL_APP_UPDATES)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_UPDATE_ID, builder.build())
        } catch (e: SecurityException) {
            // Notification permission might have been revoked at runtime
        }
    }

    fun dismissUpdateNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_UPDATE_ID)
        } catch (_: Exception) {}
    }
}
