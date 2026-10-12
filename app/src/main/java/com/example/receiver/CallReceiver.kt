package com.example.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.QuickCallApp
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.service.OverlayService
import com.example.utils.PermissionHelper
import com.example.utils.SpamDetector
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown"

        val app = context.applicationContext as? QuickCallApp ?: return

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                handleIncomingCall(context, app, incomingNumber)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended or rejected -> stop overlay
                stopOverlay(context)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered -> can update overlay state or keep it minimized
            }
        }
    }

    private fun handleIncomingCall(context: Context, app: QuickCallApp, number: String) {
        app.applicationScope.launch {
            val callerInfo = app.repository.getCaller(number)
            val analysis = SpamDetector.analyze(callerInfo)

            // Record call in database
            app.repository.addCallRecord(
                CallRecord(
                    phoneNumber = number,
                    callerName = callerInfo?.name ?: "Unknown",
                    callType = if (analysis.shouldAutoBlock) CallType.SPAM_BLOCKED else CallType.INCOMING,
                    isSpam = analysis.score >= 50,
                    spamReason = if (analysis.score >= 50) analysis.warningReason else null
                )
            )

            // Check if floating overlay permission is granted
            if (PermissionHelper.canDrawOverlays(context)) {
                startOverlayService(
                    context = context,
                    number = number,
                    name = callerInfo?.name ?: "Unknown Caller",
                    category = callerInfo?.category ?: "Cellular Call",
                    spamScore = analysis.score,
                    isSpam = analysis.score >= 50,
                    city = callerInfo?.city ?: "Unknown City",
                    carrier = callerInfo?.carrier ?: "Wireless"
                )
            } else {
                // Post high-priority Heads-up Notification
                showCallerNotification(
                    context = context,
                    number = number,
                    name = callerInfo?.name ?: "Incoming Call",
                    isSpam = analysis.score >= 50,
                    warning = analysis.warningReason
                )
            }
        }
    }

    private fun startOverlayService(
        context: Context,
        number: String,
        name: String,
        category: String,
        spamScore: Int,
        isSpam: Boolean,
        city: String,
        carrier: String
    ) {
        val serviceIntent = Intent(context, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_PHONE_NUMBER, number)
            putExtra(OverlayService.EXTRA_CALLER_NAME, name)
            putExtra(OverlayService.EXTRA_CATEGORY, category)
            putExtra(OverlayService.EXTRA_SPAM_SCORE, spamScore)
            putExtra(OverlayService.EXTRA_IS_SPAM, isSpam)
            putExtra(OverlayService.EXTRA_CITY, city)
            putExtra(OverlayService.EXTRA_CARRIER, carrier)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun stopOverlay(context: Context) {
        val serviceIntent = Intent(context, OverlayService::class.java)
        context.stopService(serviceIntent)
    }

    private fun showCallerNotification(
        context: Context,
        number: String,
        name: String,
        isSpam: Boolean,
        warning: String
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TARGET", "caller_popup")
            putExtra("EXTRA_PHONE_NUMBER", number)
            putExtra("EXTRA_CALLER_NAME", name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, QuickCallApp.CHANNEL_CALLER_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(if (isSpam) "⚠️ SPAM ALERT: $name" else "📞 $name")
            .setContentText(if (isSpam) warning else "Incoming call from $number")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(9001, notification)
        }
    }
}
