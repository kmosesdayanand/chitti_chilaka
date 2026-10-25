package com.example.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-schedule hourly and midnight WorkManager job
            CountdownWorker.schedulePeriodicUpdates(context)

            // Immediately refresh widgets with freshly computed time left
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    WidgetUpdater.updateAllWidgets(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
