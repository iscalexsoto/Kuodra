package com.arenacun.kuodra

import android.app.Application
import com.arenacun.kuodra.data.sync.WorkManagerSyncTrigger
import com.arenacun.kuodra.di.appModule
import com.arenacun.kuodra.di.dataModule
import com.arenacun.kuodra.di.networkModule
import com.arenacun.kuodra.di.presentationModule
import com.arenacun.kuodra.presentation.crash.CrashHandler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KuodraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Captura crashes y los muestra en pantalla (ver CrashActivity) en vez de morir en silencio.
        CrashHandler.install(this)

        // La CrashActivity corre en el proceso ":crash"; ahí no necesitamos Koin (la pantalla
        // de crash no lo usa) y reinicializarlo solo añadiría puntos de fallo.
        if (getProcessName() != "$packageName:crash") {
            val koin = startKoin {
                androidLogger()
                androidContext(this@KuodraApplication)
                modules(appModule, networkModule, dataModule, presentationModule)
            }.koin
            // Sincronización periódica de respaldo (la sesión se valida dentro del worker).
            koin.get<WorkManagerSyncTrigger>().schedulePeriodic()
        }
    }
}
