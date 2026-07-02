package com.arenacun.kuodra.di

import com.arenacun.kuodra.data.local.db.KuodraDatabase
import com.arenacun.kuodra.data.remote.KtorTelemetryApi
import com.arenacun.kuodra.data.remote.TelemetryApi
import com.arenacun.kuodra.data.telemetry.CrashSpool
import com.arenacun.kuodra.data.telemetry.DeviceContextProvider
import com.arenacun.kuodra.data.telemetry.PocketBaseTelemetry
import com.arenacun.kuodra.data.telemetry.TelemetryTrigger
import com.arenacun.kuodra.data.telemetry.TelemetryUploader
import com.arenacun.kuodra.data.telemetry.WorkManagerTelemetryTrigger
import com.arenacun.kuodra.domain.telemetry.Telemetry
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

/**
 * Observabilidad remota (telemetría casera sobre PocketBase), detrás del puerto neutral [Telemetry].
 * Para migrar a Sentry: crear `SentryTelemetry : Telemetry` y cambiar el binding de abajo. Nada más.
 */
val telemetryModule = module {
    single { Json { ignoreUnknownKeys = true; encodeDefaults = true } }
    single { DeviceContextProvider(androidContext()) }
    single { CrashSpool(File(androidContext().filesDir, "telemetry-crash"), get()) }
    single { KtorTelemetryApi(get()) } bind TelemetryApi::class
    single { WorkManagerTelemetryTrigger(androidContext()) } bind TelemetryTrigger::class
    single { TelemetryUploader(get(), get(), get(), get(), get()) }
    single { PocketBaseTelemetry(get(), get(), get(), get(), get()) } bind Telemetry::class
}
