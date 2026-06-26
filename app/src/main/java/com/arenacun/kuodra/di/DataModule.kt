package com.arenacun.kuodra.di

import com.arenacun.kuodra.data.repository.AuthRepositoryImpl
import com.arenacun.kuodra.data.repository.MovementRepositoryImpl
import com.arenacun.kuodra.data.repository.PreferencesRepositoryImpl
import com.arenacun.kuodra.data.repository.SettingsRepositoryImpl
import com.arenacun.kuodra.data.repository.SpaceRepositoryImpl
import com.arenacun.kuodra.data.repository.SummaryRepositoryImpl
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PreferencesRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import org.koin.dsl.bind
import org.koin.dsl.module

/** Implementaciones de los contratos de dominio (capa data). */
val dataModule = module {
    single { AuthRepositoryImpl() } bind AuthRepository::class
    single { SpaceRepositoryImpl() } bind SpaceRepository::class
    single { MovementRepositoryImpl(get()) } bind MovementRepository::class
    single { SummaryRepositoryImpl(get()) } bind SummaryRepository::class
    single { SettingsRepositoryImpl(get()) } bind SettingsRepository::class
    single { PreferencesRepositoryImpl() } bind PreferencesRepository::class
}
