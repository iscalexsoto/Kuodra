package com.arenacun.kuodra.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker que solo dispara [SyncManager.sync]. WorkManager se encarga de la restricción de red, el
 * reintento con backoff y la ejecución diferida; la lógica vive en [SyncManager].
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val syncManager: SyncManager by inject()

    override suspend fun doWork(): Result =
        syncManager.sync().fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
}
