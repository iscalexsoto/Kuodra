package com.arenacun.kuodra.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.repository.PreferencesRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de alcance app: expone el tema y resuelve el destino inicial según la
 * sesión persistida y si el onboarding ya se completó.
 */
class AppViewModel(
    preferences: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    val darkTheme: StateFlow<Boolean> = preferences.darkTheme

    private val _start = MutableStateFlow(StartState.Loading)
    val start: StateFlow<StartState> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            val session = authRepository.restoreSession()
            _start.value = when {
                session == null -> StartState.LoggedOut
                session.name.isBlank() -> StartState.NeedsName
                !spaceRepository.isConfigured() -> StartState.Onboarding
                else -> StartState.Ready
            }
        }
    }
}

/** Estado de arranque que decide la pantalla inicial. */
enum class StartState { Loading, LoggedOut, NeedsName, Onboarding, Ready }
