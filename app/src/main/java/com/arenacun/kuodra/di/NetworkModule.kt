package com.arenacun.kuodra.di

import com.arenacun.kuodra.data.remote.AuthApi
import com.arenacun.kuodra.data.remote.CategoryApi
import com.arenacun.kuodra.data.remote.KtorAuthApi
import com.arenacun.kuodra.data.remote.KtorCategoryApi
import com.arenacun.kuodra.data.remote.KtorMovementApi
import com.arenacun.kuodra.data.remote.MovementApi
import com.arenacun.kuodra.data.remote.PocketBaseClient
import org.koin.dsl.bind
import org.koin.dsl.module

/** Cliente HTTP y APIs remotas (PocketBase). La URL base viene de `BuildConfig`. */
val networkModule = module {
    single { PocketBaseClient() }
    single { KtorAuthApi(get()) } bind AuthApi::class
    single { KtorMovementApi(get()) } bind MovementApi::class
    single { KtorCategoryApi(get()) } bind CategoryApi::class
}
