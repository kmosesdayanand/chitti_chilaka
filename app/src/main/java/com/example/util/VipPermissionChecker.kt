package com.example.util

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class VipPermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isCritical: Boolean = true
)

object VipPermissionChecker {

    fun isContactsPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isCallScreeningRoleHeld(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
        } else {
            // Android 9 and lower uses PHONE_STATE broadcast receiver fallback
            true
        }
    }

    fun isNotificationPolicyAccessGranted(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.isNotificationPolicyAccessGranted ?: false
    }

    fun isPhoneStatePermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isBatteryOptimizationExempted(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * Checks if all required items are granted so VIP ring can function reliably.
     */
    fun hasAllEssentialPermissions(context: Context): Boolean {
        val contacts = isContactsPermissionGranted(context)
        val role = isCallScreeningRoleHeld(context)
        val dnd = isNotificationPolicyAccessGranted(context)
        val phoneState = isPhoneStatePermissionGranted(context)
        return contacts && role && dnd && phoneState
    }

    fun getChecklist(context: Context): List<VipPermissionItem> {
        return listOf(
            VipPermissionItem(
                id = "contacts",
                title = "Contacts Permission",
                description = "Required to pick VIP contacts from your address book and match incoming calls against their numbers.",
                isGranted = isContactsPermissionGranted(context),
                isCritical = true
            ),
            VipPermissionItem(
                id = "role_screening",
                title = "Call Screening Role",
                description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "Allows Sentry to identify the incoming number instantly without reading your personal call logs. Sentry never blocks or rejects calls."
                } else {
                    "Not required on Android 9 and below. Sentry uses the system phone state receiver."
                },
                isGranted = isCallScreeningRoleHeld(context),
                isCritical = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ),
            VipPermissionItem(
                id = "dnd_access",
                title = "Do Not Disturb (Notification Policy)",
                description = "Allows Sentry to bypass Do Not Disturb mode and switch ringer mode to Normal so VIP emergency calls ring loud.",
                isGranted = isNotificationPolicyAccessGranted(context),
                isCritical = true
            ),
            VipPermissionItem(
                id = "phone_state",
                title = "Phone State Permission",
                description = "Needed to detect when you answer the call or hang up, so Sentry immediately restores your previous ringer volume.",
                isGranted = isPhoneStatePermissionGranted(context),
                isCritical = true
            ),
            VipPermissionItem(
                id = "battery_opt",
                title = "Battery Optimization Exemption",
                description = "Prevents aggressive Android battery savers from delaying the VIP ring while the screen is locked.",
                isGranted = isBatteryOptimizationExempted(context),
                isCritical = false
            )
        )
    }

    fun createDndSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createBatteryOptimizationIntent(context: Context): Intent {
        return try {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (_: Exception) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}
