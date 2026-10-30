package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Short-lived background component to monitor call state transitions (OFFHOOK, IDLE)
 * and safely restore the user's previous ringer volume and DND mode when the call ends or is answered.
 *
 * Implements:
 * - TelephonyCallback (API 31+)
 * - PhoneStateListener (API < 31)
 * - 60-second safety timeout to prevent stuck max-volume if call events are dropped
 */
class VipRingService : Service() {

    companion object {
        private const val TAG = "VipRingService"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "sentry_vip_ring_channel"

        fun start(context: Context, contactName: String) {
            try {
                val intent = Intent(context, VipRingService::class.java).apply {
                    putExtra(EXTRA_CONTACT_NAME, contactName)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        context.startForegroundService(intent)
                    } catch (_: Exception) {
                        context.startService(intent)
                    }
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VipRingService", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, VipRingService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop VipRingService", e)
            }
        }
    }

    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var timeoutJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val contactName = intent?.getStringExtra(EXTRA_CONTACT_NAME) ?: "VIP Contact"
        createNotificationChannel()

        val notification = buildNotification(contactName)
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not startForeground, running as standard service", e)
        }

        registerCallStateListener()
        startSafetyTimeout()

        return START_NOT_STICKY
    }

    private fun registerCallStateListener() {
        try {
            val tm = telephonyManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallState(state)
                    }
                }
                telephonyCallback = callback
                tm.registerTelephonyCallback(mainExecutor, callback)
                Log.d(TAG, "Registered TelephonyCallback for call state monitoring.")
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallState(state)
                    }
                }
                phoneStateListener = listener
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                Log.d(TAG, "Registered PhoneStateListener for call state monitoring.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering telephony listener", e)
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.i(TAG, "Call answered (OFFHOOK). Restoring audio state.")
                VipRingManager.restorePreviousState(applicationContext)
                stopSelf()
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.i(TAG, "Call ended or missed (IDLE). Restoring audio state.")
                VipRingManager.restorePreviousState(applicationContext)
                stopSelf()
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d(TAG, "Phone is still ringing.")
            }
        }
    }

    private fun startSafetyTimeout() {
        timeoutJob?.cancel()
        // 60-second fallback: if Telecom fails to notify IDLE/OFFHOOK, restore state automatically
        timeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(60_000L)
            Log.w(TAG, "VIP ring 60s timeout reached. Auto-restoring audio state.")
            VipRingManager.restorePreviousState(applicationContext)
            stopSelf()
        }
    }

    private fun unregisterCallStateListener() {
        try {
            val tm = telephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let {
                    tm?.unregisterTelephonyCallback(it)
                }
                telephonyCallback = null
            } else {
                phoneStateListener?.let {
                    @Suppress("DEPRECATION")
                    tm?.listen(it, PhoneStateListener.LISTEN_NONE)
                }
                phoneStateListener = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering telephony listener", e)
        }
    }

    override fun onDestroy() {
        timeoutJob?.cancel()
        unregisterCallStateListener()
        super.onDestroy()
        Log.d(TAG, "VipRingService destroyed.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sentry VIP Emergency Ring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when Sentry is actively boosting ring volume for a VIP contact."
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contactName: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("VIP Call Ringing: $contactName")
            .setContentText("Volume boosted to ensure emergency call audibility")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
