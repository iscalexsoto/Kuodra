package com.arenacun.kuodra.presentation.app

import androidx.lifecycle.ViewModel
import com.arenacun.kuodra.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.StateFlow

/** ViewModel de alcance app: expone el tema para el `KuodraRoot`. */
class AppViewModel(
    preferences: PreferencesRepository,
) : ViewModel() {
    val darkTheme: StateFlow<Boolean> = preferences.darkTheme
}
