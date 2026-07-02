package com.arenacun.kuodra.data.telemetry

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Encola un [TelemetryUploadWorker] único *debounced* con restricción de red (mismo patrón que
 * [com.arenacun.kuodra.data.sync.WorkManagerSyncTrigger]). Las ráfagas de eventos colapsan en una
 * sola subida; WorkManager reintenta con backoff si falla.
 */
class WorkManagerTelemetryTrigger(private val context: Context) : TelemetryTrigger {

    override fun requestUpload() {
        val request = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setInitialDelay(DEBOUNCE_SECONDS, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }

    private companion object {
        const val WORK = "kuodra-telemetry"
        const val DEBOUNCE_SECONDS = 5L
    }
}
