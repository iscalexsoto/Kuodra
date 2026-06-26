package com.arenacun.kuodra.di

import com.arenacun.kuodra.data.local.KuodraSeedSource
import org.koin.dsl.module

/** Singletons base (fuentes de datos en memoria). */
val appModule = module {
    single { KuodraSeedSource() }
}
