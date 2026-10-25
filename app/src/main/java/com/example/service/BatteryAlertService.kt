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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AlertRule
import com.example.data.AppDatabase
import com.example.util.AudioAlertPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BatteryAlertService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                handleBatteryUpdate(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForegroundServiceNotification()

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            AudioAlertPlayer.stop()
            dismissTriggerNotification()
            return START_STICKY
        }

        // Verify if any rules are active; if not, stop service
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val enabledRules = db.alertRuleDao().getEnabledRulesSync()
            if (enabledRules.isEmpty()) {
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun handleBatteryUpdate(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return
        val batteryPct = (level * 100) / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val dao = db.alertRuleDao()
            val enabledRules = dao.getEnabledRulesSync()

            if (enabledRules.isEmpty()) {
                stopSelf()
                return@launch
            }

            for (rule in enabledRules) {
                if (rule.direction == AlertRule.DIRECTION_CHARGING_UP) {
                    // Charging up crossing
                    if (isCharging && batteryPct >= rule.level) {
                        if (!rule.lastTriggered) {
                            dao.updateTriggeredState(rule.id, true)
                            triggerAlert(rule, batteryPct)
                        }
                    } else if (!isCharging || batteryPct < rule.level) {
                        // Reset crossing
                        if (rule.lastTriggered) {
                            dao.updateTriggeredState(rule.id, false)
                        }
                    }
                } else {
                    // Draining down crossing
                    if (!isCharging && batteryPct <= rule.level) {
                        if (!rule.lastTriggered) {
                            dao.updateTriggeredState(rule.id, true)
                            triggerAlert(rule, batteryPct)
                        }
                    } else if (isCharging || batteryPct > rule.level) {
                        // Reset crossing
                        if (rule.lastTriggered) {
                            dao.updateTriggeredState(rule.id, false)
                        }
                    }
                }
            }
        }
    }

    private fun triggerAlert(rule: AlertRule, batteryPct: Int) {
        serviceScope.launch(Dispatchers.Main) {
            showTriggerNotification(rule, batteryPct)
            AudioAlertPlayer.playSound(
                context = applicationContext,
                soundType = rule.soundType,
                soundUriString = rule.soundUri,
                volume = rule.volume,
                startMs = rule.startMs,
                endMs = rule.endMs,
                onStopCallback = {
                    dismissTriggerNotification()
                }
            )
        }
    }

    private fun startForegroundServiceNotification() {
        val notification = createOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID_SERVICE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID_SERVICE, notification)
        }
    }

    private fun createOngoingNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setContentTitle("Phone Manager")
            .setContentText("Watching battery level")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showTriggerNotification(rule: AlertRule, batteryPct: Int) {
        val stopIntent = Intent(this, BatteryAlertService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val directionText = if (rule.direction == AlertRule.DIRECTION_CHARGING_UP) "charged up to" else "drained down to"

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setContentTitle("Battery Alert: $batteryPct%")
            .setContentText("Battery has $directionText ${rule.level}%")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Sound", stopPendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_TRIGGER, notification)
    }

    private fun dismissTriggerNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID_TRIGGER)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR,
                "Battery Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification while battery rules are being monitored"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "Battery Level Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts triggered when battery level thresholds are reached"
                enableVibration(true)
            }

            manager.createNotificationChannel(monitorChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        AudioAlertPlayer.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_MONITOR = "channel_battery_monitor"
        const val CHANNEL_ALERT = "channel_battery_alerts"
        const val NOTIFICATION_ID_SERVICE = 1001
        const val NOTIFICATION_ID_TRIGGER = 2001
        const val ACTION_STOP_ALARM = "com.example.action.STOP_ALARM"

        fun syncWithDatabase(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val enabledRules = db.alertRuleDao().getEnabledRulesSync()
                val intent = Intent(context, BatteryAlertService::class.java)
                if (enabledRules.isNotEmpty()) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        context.stopService(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
