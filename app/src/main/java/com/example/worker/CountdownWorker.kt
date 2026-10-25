package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.widget.WidgetUpdater
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CountdownWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            WidgetUpdater.updateAllWidgets(appContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_WORK_TAG = "countdown_widget_periodic_worker"

        fun schedulePeriodicUpdates(context: Context) {
            try {
                val initialDelay = computeInitialDelayToNextHourOrMidnight()

                val workRequest = PeriodicWorkRequestBuilder<CountdownWorker>(
                    1, TimeUnit.HOURS,
                    15, TimeUnit.MINUTES
                )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(PERIODIC_WORK_TAG)
                .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_TAG,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun computeInitialDelayToNextHourOrMidnight(): Long {
            val now = Calendar.getInstance()
            val nextHour = (now.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delay = nextHour.timeInMillis - now.timeInMillis
            return delay.coerceAtLeast(30000L)
        }
    }
}
