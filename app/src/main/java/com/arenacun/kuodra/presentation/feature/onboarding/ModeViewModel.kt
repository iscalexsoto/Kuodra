package com.arenacun.kuodra.presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ModeViewModel(
    private val spaceRepository: SpaceRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    /** Nombre del usuario para el saludo; reactivo para reflejar el guardado reciente. */
    val userName: StateFlow<String> = authRepository.session
        .map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.session.value?.name.orEmpty())

    private val _selectedMode = MutableStateFlow<UseCase?>(null)
    val selectedMode = _selectedMode.asStateFlow()

    fun onSelect(mode: UseCase) {
        _selectedMode.value = mode
    }

    /** Camino "solo personal": el espacio queda listo sin pasar por Crear. */
    fun selectPersonal() {
        spaceRepository.selectUseCase(UseCase.Personal)
    }
}
