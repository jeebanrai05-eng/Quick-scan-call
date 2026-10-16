package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.service.CallDetectionManager

/**
 * BroadcastReceiver for android.intent.action.PHONE_STATE.
 * Coordinates with CallDetectionManager to ensure PhoneStateListener and broadcast
 * state transitions seamlessly trigger caller detection and floating overlay display.
 */
class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d("CallReceiver", "ACTION_PHONE_STATE_CHANGED: state=$stateStr, number=$incomingNumber")

        val state = when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            else -> return
        }

        CallDetectionManager.handleCallState(context, state, incomingNumber)
    }
}
