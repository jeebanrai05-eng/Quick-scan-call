package com.example.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.QuickCallApp
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.utils.PermissionHelper
import com.example.utils.SpamDetector
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CallDetectionManager manages incoming call detection using Android's TelephonyManager
 * and PhoneStateListener API (with modern TelephonyCallback support on API 31+).
 *
 * When an incoming call is detected (CALL_STATE_RINGING):
 * 1. Resolves caller details and carrier from local database.
 * 2. Evaluates spam risk with SpamDetector.
 * 3. Records the call in the local call history database.
 * 4. Displays a draggable floating overlay window (OverlayService) if overlay permission is granted,
 *    or a high-priority heads-up notification otherwise.
 *
 * When the call ends (CALL_STATE_IDLE):
 * Automatically removes and closes the floating overlay.
 */
object CallDetectionManager {

    private const val TAG = "CallDetectionManager"
    private const val NOTIFICATION_CALLER_ID = 9001
    private const val DEBOUNCE_WINDOW_MS = 2500L

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: IncomingCallPhoneStateListener? = null
    private var telephonyCallback: Any? = null // Holds TelephonyCallback on API 31+

    private val isListening = AtomicBoolean(false)
    private var lastRingingTimestamp = 0L
    private var lastRingingNumber: String? = null

    /**
     * Subclass of PhoneStateListener to monitor call state changes using TelephonyManager.
     */
    @Suppress("DEPRECATION")
    private class IncomingCallPhoneStateListener(
        private val appContext: Context
    ) : PhoneStateListener() {

        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            Log.d(TAG, "PhoneStateListener.onCallStateChanged: state=$state, number=$phoneNumber")
            handleCallState(appContext, state, phoneNumber)
        }
    }

    /**
     * Android 12 (API 31+) TelephonyCallback implementation.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private class IncomingCallTelephonyCallback(
        private val appContext: Context
    ) : TelephonyCallback(), TelephonyCallback.CallStateListener {

        override fun onCallStateChanged(state: Int) {
            Log.d(TAG, "TelephonyCallback.onCallStateChanged: state=$state")
            // Note: TelephonyCallback does not pass phoneNumber directly in onCallStateChanged(int)
            // It triggers state change, number is coordinated via cached or broadcast state
            handleCallState(appContext, state, null)
        }
    }

    /**
     * Start listening for incoming phone call states using TelephonyManager & PhoneStateListener.
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun startListening(context: Context) {
        val appContext = context.applicationContext
        if (isListening.get()) {
            Log.d(TAG, "Already listening to PhoneState")
            return
        }

        val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (tm == null) {
            Log.w(TAG, "TelephonyManager not available on this device")
            return
        }
        telephonyManager = tm

        try {
            // Register PhoneStateListener (works reliably across Android versions)
            @Suppress("DEPRECATION")
            val listener = IncomingCallPhoneStateListener(appContext).also {
                phoneStateListener = it
                tm.listen(it, PhoneStateListener.LISTEN_CALL_STATE)
            }

            // On Android 12+ (API 31+), additionally register TelephonyCallback if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val executor = appContext.mainExecutor
                    val callback = IncomingCallTelephonyCallback(appContext)
                    telephonyCallback = callback
                    tm.registerTelephonyCallback(executor, callback)
                    Log.d(TAG, "Registered TelephonyCallback on API ${Build.VERSION.SDK_INT}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to register TelephonyCallback: ${e.message}")
                }
            }

            isListening.set(true)
            Log.i(TAG, "Successfully started PhoneStateListener on TelephonyManager")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: READ_PHONE_STATE permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering PhoneStateListener: ${e.message}")
        }
    }

    /**
     * Stop listening to call state changes.
     */
    @Synchronized
    fun stopListening() {
        val tm = telephonyManager
        if (tm != null) {
            phoneStateListener?.let {
                @Suppress("DEPRECATION")
                try {
                    tm.listen(it, PhoneStateListener.LISTEN_NONE)
                } catch (e: Exception) {
                    Log.w(TAG, "Error unregistering PhoneStateListener: ${e.message}")
                }
                phoneStateListener = null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { callback ->
                    try {
                        tm.unregisterTelephonyCallback(callback as TelephonyCallback)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error unregistering TelephonyCallback: ${e.message}")
                    }
                    telephonyCallback = null
                }
            }
        }
        isListening.set(false)
        Log.i(TAG, "Stopped PhoneStateListener")
    }

    /**
     * Centralized call state processor.
     */
    fun handleCallState(context: Context, state: Int, incomingNumber: String?) {
        val appContext = context.applicationContext
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                val number = incomingNumber?.takeIf { it.isNotBlank() } ?: lastRingingNumber ?: "Unknown"
                processIncomingCall(appContext, number)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d(TAG, "Call State is IDLE: Dismissing floating overlay")
                lastRingingNumber = null
                dismissFloatingOverlay(appContext)
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d(TAG, "Call State is OFFHOOK: Call answered / in progress")
            }
        }
    }

    /**
     * Processes an incoming call: performs spam detection, writes to DB, and displays floating overlay.
     */
    fun processIncomingCall(context: Context, number: String) {
        val now = System.currentTimeMillis()
        if (number == lastRingingNumber && (now - lastRingingTimestamp) < DEBOUNCE_WINDOW_MS) {
            Log.d(TAG, "Debouncing duplicate incoming call event for $number")
            return
        }
        lastRingingTimestamp = now
        lastRingingNumber = number

        val app = context.applicationContext as? QuickCallApp ?: return
        val prefs = context.getSharedPreferences("quick_call_prefs", Context.MODE_PRIVATE)
        val overlayEnabledInSettings = prefs.getBoolean("floating_overlay", true)

        app.applicationScope.launch {
            val callerInfo = app.repository.getCaller(number)
            val analysis = SpamDetector.analyze(callerInfo)

            val callerName = callerInfo?.name ?: "Unknown Caller"
            val category = callerInfo?.category ?: "Cellular Call"
            val city = callerInfo?.city ?: "Local"
            val carrier = callerInfo?.carrier ?: "Wireless"
            val isSpam = analysis.score >= 50
            val spamScore = analysis.score

            // Record call in database
            app.repository.addCallRecord(
                CallRecord(
                    phoneNumber = number,
                    callerName = callerName,
                    callType = if (analysis.shouldAutoBlock) CallType.SPAM_BLOCKED else CallType.INCOMING,
                    isSpam = isSpam,
                    spamReason = if (isSpam) analysis.warningReason else null
                )
            )

            // Display floating overlay if permission granted and enabled
            if (overlayEnabledInSettings && PermissionHelper.canDrawOverlays(context)) {
                displayFloatingOverlay(
                    context = context,
                    number = number,
                    name = callerName,
                    category = category,
                    spamScore = spamScore,
                    isSpam = isSpam,
                    city = city,
                    carrier = carrier
                )
            } else {
                // Post high-priority Heads-up Notification as fallback
                showCallerNotification(
                    context = context,
                    number = number,
                    name = callerName,
                    isSpam = isSpam,
                    warning = analysis.warningReason
                )
            }
        }
    }

    /**
     * Launches the foreground OverlayService to render the floating Truecaller-style window.
     */
    fun displayFloatingOverlay(
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
            action = OverlayService.ACTION_SHOW_OVERLAY
            putExtra(OverlayService.EXTRA_PHONE_NUMBER, number)
            putExtra(OverlayService.EXTRA_CALLER_NAME, name)
            putExtra(OverlayService.EXTRA_CATEGORY, category)
            putExtra(OverlayService.EXTRA_SPAM_SCORE, spamScore)
            putExtra(OverlayService.EXTRA_IS_SPAM, isSpam)
            putExtra(OverlayService.EXTRA_CITY, city)
            putExtra(OverlayService.EXTRA_CARRIER, carrier)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.i(TAG, "Dispatched OverlayService for incoming caller: $number ($name)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OverlayService: ${e.message}", e)
        }
    }

    /**
     * Dismisses the floating overlay by sending an intent to OverlayService.
     */
    fun dismissFloatingOverlay(context: Context) {
        try {
            val serviceIntent = Intent(context, OverlayService::class.java).apply {
                action = OverlayService.ACTION_HIDE_OVERLAY
            }
            context.stopService(serviceIntent)
            Log.d(TAG, "Stopped OverlayService successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop OverlayService: ${e.message}")
        }
    }

    /**
     * Fallback high-priority Heads-Up notification when overlay permission is not yet granted.
     */
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
            .setContentTitle(if (isSpam) "⚠️ SPAM CALL ALERT: $name" else "⚡ CALLER DETECTED: $name")
            .setContentText(if (isSpam) warning else "Incoming call from $number")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_CALLER_ID, notification)
        }
    }

    /**
     * Developer & user simulation helper to test the floating overlay directly.
     */
    fun simulateIncomingCall(
        context: Context,
        number: String = "+1 (800) 555-0199",
        name: String = "Global Express Logistics",
        category: String = "Verified Business",
        spamScore: Int = 12,
        isSpam: Boolean = false,
        city: String = "San Francisco, CA",
        carrier: String = "Verizon Wireless"
    ) {
        displayFloatingOverlay(
            context = context,
            number = number,
            name = name,
            category = category,
            spamScore = spamScore,
            isSpam = isSpam,
            city = city,
            carrier = carrier
        )
    }
}
