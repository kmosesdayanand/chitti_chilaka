package com.example

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.service.BatteryAlertService
import com.example.service.VipRingManager
import com.example.util.AppLog
import com.example.widget.WidgetUpdater
import com.example.worker.CountdownWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhoneManagerApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            CountdownWorker.schedulePeriodicUpdates(this)
            VipRingManager.init(this)
            VipRingManager.restoreIfLeftActive(this, isBootRecovery = false)

            // Guard battery service sync: only start if app is at least STARTED (foreground)
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    BatteryAlertService.syncWithDatabase(this@PhoneManagerApplication)
                }
            })
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                BatteryAlertService.syncWithDatabase(this)
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    WidgetUpdater.updateAllWidgets(this@PhoneManagerApplication)
                } catch (e: Exception) {
                    AppLog.e("PhoneManagerApplication", "Failed to update widgets", e)
                }
            }
        } catch (e: Exception) {
            AppLog.e("PhoneManagerApplication", "Error during application onCreate startup", e)
        }
    }
}
