package com.arenacun.kuodra.di

import com.arenacun.kuodra.data.remote.AuthApi
import com.arenacun.kuodra.data.remote.BudgetApi
import com.arenacun.kuodra.data.remote.CategoryApi
import com.arenacun.kuodra.data.remote.KtorAuthApi
import com.arenacun.kuodra.data.remote.KtorBudgetApi
import com.arenacun.kuodra.data.remote.KtorCategoryApi
import com.arenacun.kuodra.data.remote.KtorMovementApi
import com.arenacun.kuodra.data.remote.KtorPeriodSnapshotApi
import com.arenacun.kuodra.data.remote.KtorPersonApi
import com.arenacun.kuodra.data.remote.KtorSettlementApi
import com.arenacun.kuodra.data.remote.KtorSpaceApi
import com.arenacun.kuodra.data.remote.MovementApi
import com.arenacun.kuodra.data.remote.PeriodSnapshotApi
import com.arenacun.kuodra.data.remote.PersonApi
import com.arenacun.kuodra.data.remote.SettlementApi
import com.arenacun.kuodra.data.remote.SpaceApi
import com.arenacun.kuodra.data.remote.KtorTicketAnalysisApi
import com.arenacun.kuodra.data.remote.PocketBaseClient
import com.arenacun.kuodra.data.remote.TicketAnalysisApi
import org.koin.dsl.bind
import org.koin.dsl.module

/** Cliente HTTP y APIs remotas (PocketBase). La URL base viene de `BuildConfig`. */
val networkModule = module {
    single { PocketBaseClient() }
    single { KtorAuthApi(get()) } bind AuthApi::class
    single { KtorMovementApi(get()) } bind MovementApi::class
    single { KtorCategoryApi(get()) } bind CategoryApi::class
    single { KtorBudgetApi(get()) } bind BudgetApi::class
    single { KtorPeriodSnapshotApi(get()) } bind PeriodSnapshotApi::class
    single { KtorSpaceApi(get()) } bind SpaceApi::class
    single { KtorPersonApi(get()) } bind PersonApi::class
    single { KtorSettlementApi(get()) } bind SettlementApi::class
    single { KtorTicketAnalysisApi(get()) } bind TicketAnalysisApi::class
}
