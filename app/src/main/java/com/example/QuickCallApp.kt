package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.repository.CallerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickCallApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var repository: CallerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = CallerRepository(this)

        createNotificationChannels()

        // Start TelephonyManager PhoneStateListener for real-time incoming call detection
        com.example.service.CallDetectionManager.startListening(this)

        // Sync real device contacts and call logs
        applicationScope.launch {
            repository.syncDeviceContacts()
            repository.syncSystemCallLogs()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val callChannel = NotificationChannel(
                CHANNEL_CALLER_ID,
                "Caller ID & Spam Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows real-time caller identification and spam warnings"
                enableVibration(true)
            }

            val overlayServiceChannel = NotificationChannel(
                CHANNEL_OVERLAY_SERVICE,
                "Caller Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps floating caller identification overlay active during phone calls"
            }

            manager.createNotificationChannel(callChannel)
            manager.createNotificationChannel(overlayServiceChannel)
        }
    }

    companion object {
        const val CHANNEL_CALLER_ID = "channel_caller_id"
        const val CHANNEL_OVERLAY_SERVICE = "channel_overlay_service"

        lateinit var instance: QuickCallApp
            private set
    }
}
