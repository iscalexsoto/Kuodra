package com.arenacun.kuodra.presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ModeViewModel(
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

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
