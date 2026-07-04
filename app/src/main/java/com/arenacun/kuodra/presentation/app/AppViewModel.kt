package com.arenacun.kuodra.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.Session
import com.arenacun.kuodra.domain.model.ThemeMode
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.repository.PreferencesRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.telemetry.Telemetry
import com.arenacun.kuodra.presentation.navigation.Destination
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
    private val telemetry: Telemetry,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode

    private val _start = MutableStateFlow(StartState.Loading)
    val start: StateFlow<StartState> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            val session = authRepository.restoreSession()
            val state = resolveStartState(session, spaceRepository.isConfigured())
            telemetry.breadcrumb("app", "startState", mapOf("state" to state.name))
            _start.value = state
        }
    }
}

/** Estado de arranque que decide la pantalla inicial. */
enum class StartState { Loading, LoggedOut, NeedsName, Onboarding, Ready }

/**
 * Decide el estado post-autenticación a partir de la sesión y si el espacio ya está configurado.
 * Compartido entre el arranque ([AppViewModel]) y el flujo OTP en vivo ([AuthViewModel]) para
 * que ambos rutee igual y no se re-pregunte el nombre cuando el backend ya lo tiene.
 */
fun resolveStartState(session: Session?, spaceConfigured: Boolean): StartState = when {
    session == null -> StartState.LoggedOut
    session.name.isBlank() -> StartState.NeedsName
    !spaceConfigured -> StartState.Onboarding
    else -> StartState.Ready
}

/** Mapea el estado de arranque a su destino de navegación. */
internal fun StartState.toDestination(): Destination = when (this) {
    StartState.NeedsName -> Destination.Name
    StartState.Onboarding -> Destination.Mode
    StartState.Ready -> Destination.Dashboard
    // Loading no llega aquí; LoggedOut y el fallback arrancan en el flujo de auth.
    StartState.Loading, StartState.LoggedOut -> Destination.AuthGraph
}
