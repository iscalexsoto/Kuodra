package com.arenacun.kuodra.data.telemetry

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker que solo dispara [TelemetryUploader.upload]. WorkManager se encarga de la red, el backoff y
 * la ejecución diferida. Mismo patrón que [com.arenacun.kuodra.data.sync.SyncWorker].
 */
class TelemetryUploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val uploader: TelemetryUploader by inject()

    override suspend fun doWork(): Result =
        uploader.upload().fold(
            onSuccess = { Result.success() },
            onFailure = {
                Log.w("KuodraTelemetry", "Subida de telemetría fallida; se reintentará", it)
                Result.retry()
            },
        )
}
