package com.arenacun.kuodra.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Implementación de [SyncTrigger] sobre WorkManager: encola un trabajo único *debounced* (las
 * ráfagas de escrituras colapsan en una sola sincronización) con restricción de red.
 */
class WorkManagerSyncTrigger(private val context: Context) : SyncTrigger {

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints())
            .setInitialDelay(DEBOUNCE_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    /** Sincronización periódica de respaldo (llamar al arrancar la app). */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_HOURS, TimeUnit.HOURS)
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private companion object {
        const val ONE_TIME_WORK = "kuodra-sync"
        const val PERIODIC_WORK = "kuodra-sync-periodic"
        const val DEBOUNCE_SECONDS = 5L
        const val PERIODIC_HOURS = 6L
    }
}
