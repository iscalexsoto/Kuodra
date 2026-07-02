package com.arenacun.kuodra.data.telemetry

/**
 * Dispara la subida de telemetría pendiente. Interfaz para poder inyectar un [NoOp] en tests y para
 * que [PocketBaseTelemetry] no dependa de WorkManager. La impl real es [WorkManagerTelemetryTrigger].
 */
interface TelemetryTrigger {
    fun requestUpload()

    object NoOp : TelemetryTrigger {
        override fun requestUpload() = Unit
    }
}
