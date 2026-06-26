package com.arenacun.kuodra.presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.terminologyFor
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CreateSpaceViewModel(
    private val useCase: UseCase,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    /** Texto del campo; vacío muestra el default del caso de uso como placeholder. */
    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    fun onNameChange(value: String) {
        _name.value = value
    }

    fun create() {
        val finalName = _name.value.ifBlank { terminologyFor(useCase).groupName }
        spaceRepository.createSpace(useCase, finalName)
    }
}
