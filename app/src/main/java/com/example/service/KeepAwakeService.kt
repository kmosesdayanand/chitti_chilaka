package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.KeepAwakeDuration
import com.example.data.KeepAwakeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KeepAwakeService : Service() {

    companion object {
        private const val TAG = "KeepAwakeService"
        const val ACTION_START = "com.example.action.START_KEEP_AWAKE"
        const val ACTION_STOP = "com.example.action.STOP_KEEP_AWAKE"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        const val CHANNEL_ID = "sentry_keep_awake_channel"
        const val NOTIFICATION_ID = 3001

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun start(context: Context, duration: KeepAwakeDuration) {
            try {
                val intent = Intent(context, KeepAwakeService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_DURATION_MINUTES, duration.minutes)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start KeepAwakeService", e)
                try {
                    Toast.makeText(context, "Could not start Keep Awake", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, KeepAwakeService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (_: Exception) {
                try {
                    context.stopService(Intent(context, KeepAwakeService::class.java))
                } catch (_: Exception) {}
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var countdownJob: Job? = null
    private var infiniteRenewalJob: Job? = null

    private var currentDuration: KeepAwakeDuration = KeepAwakeDuration.OFF
    private var endTimeMillis: Long = 0L
    private var startTimeMillis: Long = 0L

    // Receiver to detect manual screen off
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen turned off manually by user -> stopping Keep Awake")
                stopSelf()
            }
        }
    }

    // Receiver to detect low battery (<= 15% not charging)
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100

                if (batteryPct <= 15 && !isCharging) {
                    Log.d(TAG, "Battery dropped to $batteryPct% (<= 15%) while discharging -> stopping Keep Awake")
                    stopSelf()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()

        // Register screen off receiver
        val screenFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, screenFilter)

        // Register battery receiver
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, batteryFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == ACTION_START) {
            val minutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 5)
            val newDuration = KeepAwakeDuration.entries.firstOrNull { it.minutes == minutes }
                ?: KeepAwakeDuration.FIVE_MINUTES

            val now = System.currentTimeMillis()
            if (startTimeMillis == 0L || (newDuration == KeepAwakeDuration.INFINITE && currentDuration != KeepAwakeDuration.INFINITE)) {
                startTimeMillis = now
            }

            currentDuration = newDuration
            endTimeMillis = if (newDuration == KeepAwakeDuration.INFINITE) {
                0L
            } else {
                now + newDuration.durationMs
            }

            KeepAwakeManager.onServiceStarted(newDuration, endTimeMillis, startTimeMillis)

            // Acquire screen wake lock
            acquireWakeLock(newDuration)

            // Start foreground or update notification
            postOrUpdateNotification()

            // Manage timer coroutines
            startCountdownTimer()
            startInfiniteRenewal()

            // Keep QS tile in sync
            KeepAwakeManager.requestTileUpdate(applicationContext)
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock(duration: KeepAwakeDuration) {
        releaseWakeLock()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager == null) {
            Log.e(TAG, "PowerManager not available")
            return
        }

        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "Sentry:KeepAwakeWakeLock"
        ).apply {
            setReferenceCounted(false)
        }

        val timeoutMs = if (duration == KeepAwakeDuration.INFINITE) {
            2 * 60 * 60 * 1000L // 2 hours safety limit, renewed hourly
        } else {
            duration.durationMs
        }

        try {
            wakeLock?.acquire(timeoutMs)
            Log.d(TAG, "Screen wake lock acquired for $timeoutMs ms (duration: ${duration.label})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire screen wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Screen wake lock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wake lock", e)
        } finally {
            wakeLock = null
        }
    }

    private fun postOrUpdateNotification() {
        val notification = buildOngoingNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed, falling back to notify()", e)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, KeepAwakeService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_keep_awake_on)
            .setContentTitle(getString(R.string.keep_awake_notification_title))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_keep_awake_off,
                getString(R.string.keep_awake_turn_off),
                stopPendingIntent
            )

        if (currentDuration != KeepAwakeDuration.INFINITE) {
            builder.setContentText(getString(R.string.keep_awake_status_timed))
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(endTimeMillis)
                .setShowWhen(true)
        } else {
            builder.setContentText(getString(R.string.keep_awake_status_infinite))
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
                .setWhen(startTimeMillis)
                .setShowWhen(true)
        }

        return builder.build()
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        if (currentDuration == KeepAwakeDuration.INFINITE) {
            return
        }

        countdownJob = serviceScope.launch {
            while (isActive) {
                val remainingMs = endTimeMillis - System.currentTimeMillis()
                if (remainingMs <= 0L) {
                    Log.d(TAG, "Keep Awake duration reached -> stopping service")
                    stopSelf()
                    break
                }
                delay(remainingMs)
                // Re-check real end time after the delay in case system clock shifted
                if (System.currentTimeMillis() >= endTimeMillis) {
                    Log.d(TAG, "Keep Awake duration ended naturally -> stopping service")
                    stopSelf()
                    break
                }
            }
        }
    }

    private fun startInfiniteRenewal() {
        infiniteRenewalJob?.cancel()
        if (currentDuration != KeepAwakeDuration.INFINITE) {
            return
        }

        infiniteRenewalJob = serviceScope.launch {
            while (isActive) {
                delay(60 * 60 * 1000L) // Wait 1 hour between renewals
                if (isActive && currentDuration == KeepAwakeDuration.INFINITE) {
                    try {
                        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                        if (wakeLock == null) {
                            @Suppress("DEPRECATION")
                            wakeLock = pm?.newWakeLock(
                                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                                "Sentry:KeepAwakeWakeLock"
                            )?.apply {
                                setReferenceCounted(false)
                            }
                        }
                        wakeLock?.acquire(2 * 60 * 60 * 1000L)
                        Log.d(TAG, "Infinite mode: wake lock safely renewed with 2-hour timeout")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to renew wake lock in infinite mode", e)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.keep_awake_notification_channel_name)
            val descriptionText = getString(R.string.keep_awake_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false

        // 1. Release wake lock immediately
        releaseWakeLock()

        // 2. Cancel timers and coroutines
        countdownJob?.cancel()
        countdownJob = null
        infiniteRenewalJob?.cancel()
        infiniteRenewalJob = null
        serviceScope.cancel()

        // 3. Unregister receivers
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}

        // 4. Cancel ongoing notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}

        // 5. Reset start time
        startTimeMillis = 0L

        // 6. Notify manager and refresh Quick Settings tile
        KeepAwakeManager.onServiceStopped(applicationContext)
        Log.d(TAG, "KeepAwakeService destroyed and state cleaned up")
    }
}
