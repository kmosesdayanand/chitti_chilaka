package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.service.VipRingManager

/**
 * Fallback BroadcastReceiver for Android 9 (API 28) and below.
 * Listens for ACTION_PHONE_STATE_CHANGED broadcasts with READ_PHONE_STATE.
 */
class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d(TAG, "Phone state changed: $stateStr")

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    @Suppress("DEPRECATION")
                    val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    Log.d(TAG, "Incoming ringing call detected: $incomingNumber")
                    if (!incomingNumber.isNullOrBlank()) {
                        VipRingManager.onIncomingCall(context.applicationContext, incomingNumber)
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK,
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d(TAG, "Call answered or ended ($stateStr). Restoring previous audio state.")
                    VipRingManager.restorePreviousState(context.applicationContext)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in PhoneStateReceiver", e)
        }
    }
}
