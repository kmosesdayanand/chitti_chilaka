package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.service.BatteryAlertService
import com.example.service.VipRingManager
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
            BatteryAlertService.syncWithDatabase(this)
            VipRingManager.init(this)
            VipRingManager.restoreIfLeftActive(this, isBootRecovery = false)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    WidgetUpdater.updateAllWidgets(this@PhoneManagerApplication)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
