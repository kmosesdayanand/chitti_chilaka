package com.example

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
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
            com.example.service.VipRingManager.restoreIfLeftActive(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
