package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.service.VipRingManager
import com.example.worker.CountdownWorker

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
