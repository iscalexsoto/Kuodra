package com.arenacun.kuodra.di

import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.scan.ScanSource
import com.arenacun.kuodra.presentation.app.AppViewModel
import com.arenacun.kuodra.presentation.feature.allmovements.AllMovementsViewModel
import com.arenacun.kuodra.presentation.feature.categories.CategoriesViewModel
import com.arenacun.kuodra.presentation.feature.auth.AuthViewModel
import com.arenacun.kuodra.presentation.feature.dashboard.DashboardViewModel
import com.arenacun.kuodra.presentation.feature.history.HistoryDetailViewModel
import com.arenacun.kuodra.presentation.feature.history.HistoryViewModel
import com.arenacun.kuodra.presentation.feature.movement.AddMovementViewModel
import com.arenacun.kuodra.presentation.feature.replenish.ReplenishViewModel
import com.arenacun.kuodra.presentation.feature.settings.SettingsViewModel
import com.arenacun.kuodra.presentation.feature.settle.SettleViewModel
import com.arenacun.kuodra.presentation.feature.movement.MovementDetailViewModel
import com.arenacun.kuodra.presentation.feature.onboarding.CreateSpaceViewModel
import com.arenacun.kuodra.presentation.feature.onboarding.ModeViewModel
import com.arenacun.kuodra.presentation.feature.onboarding.NameViewModel
import com.arenacun.kuodra.presentation.feature.scan.ScanDraftViewModel
import com.arenacun.kuodra.presentation.feature.scan.ScanTicketViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** ViewModels de la capa presentation. */
val presentationModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::NameViewModel)
    viewModelOf(::ModeViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::SettleViewModel)
    viewModelOf(::ReplenishViewModel)
    viewModelOf(::HistoryViewModel)
    viewModel { AllMovementsViewModel(get(), get(), get()) }

    // Escaneo de tickets: holder del draft (scope al grafo AddGraph desde el NavHost)
    viewModelOf(::ScanDraftViewModel)

    // ViewModels que reciben argumentos de ruta vía parametersOf(...)
    // editId nullable: getOrNull (la destructuración lanza con parametersOf(null)).
    viewModel { params -> AddMovementViewModel(params.getOrNull(), get(), get(), get(), get()) }
    viewModel { (source: ScanSource) -> ScanTicketViewModel(source, get(), get()) }
    viewModel { (useCase: UseCase) -> CreateSpaceViewModel(useCase, get()) }
    viewModel { (id: String) -> MovementDetailViewModel(id, get(), get(), get()) }
    viewModel { (id: String) -> HistoryDetailViewModel(id, get(), get()) }
}
