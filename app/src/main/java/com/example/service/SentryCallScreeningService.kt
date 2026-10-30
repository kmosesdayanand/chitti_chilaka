package com.example.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * CallScreeningService implementation for Android 10+ (API 29+).
 *
 * Receives the incoming caller's phone number without requiring invasive Call Log permissions.
 *
 * Strict Rules:
 * - NEVER reject calls
 * - NEVER silence calls
 * - ALWAYS respond with an empty CallResponse so call routing is unimpeded
 * - Trigger VipRingManager to evaluate if caller is an enabled VIP contact
 */
@RequiresApi(Build.VERSION_CODES.Q)
class SentryCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "CallScreeningService"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        try {
            // ALWAYS respond with empty CallResponse immediately so call is never blocked, delayed or silenced!
            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

            respondToCall(callDetails, response)
            Log.d(TAG, "Screened call responded with normal non-blocking CallResponse.")

            // Only process incoming calls
            if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
                val handle = callDetails.handle
                val incomingNumber = handle?.schemeSpecificPart
                Log.d(TAG, "Incoming call received in screening service: handle=$handle, number=$incomingNumber")

                if (!incomingNumber.isNullOrBlank()) {
                    VipRingManager.onIncomingCall(applicationContext, incomingNumber)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onScreenCall", e)
            try {
                // Failsafe response so the incoming call continues uninterrupted
                respondToCall(callDetails, CallResponse.Builder().build())
            } catch (_: Exception) {}
        }
    }
}
