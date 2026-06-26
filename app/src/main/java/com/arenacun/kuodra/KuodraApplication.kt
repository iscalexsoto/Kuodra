package com.arenacun.kuodra

import android.app.Application
import com.arenacun.kuodra.di.appModule
import com.arenacun.kuodra.di.dataModule
import com.arenacun.kuodra.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KuodraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@KuodraApplication)
            modules(appModule, dataModule, presentationModule)
        }
    }
}
