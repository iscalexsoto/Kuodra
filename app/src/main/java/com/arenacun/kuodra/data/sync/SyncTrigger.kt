package com.arenacun.kuodra.data.sync

/**
 * Dispara una sincronización. Abstrae WorkManager para que los repositorios no dependan de él
 * (y sean testeables). La impl real encola un trabajo *debounced* con restricción de red.
 */
interface SyncTrigger {
    fun requestSync()

    companion object {
        /** No-op para tests y para AuthRepositoryImpl cuando no se inyecta sincronización. */
        val NoOp: SyncTrigger = object : SyncTrigger {
            override fun requestSync() = Unit
        }
    }
}
