package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpaceRepositoryImpl : SpaceRepository {

    private val state = MutableStateFlow(Space(UseCase.Gastos))
    override val activeSpace: StateFlow<Space> = state.asStateFlow()

    override fun selectUseCase(useCase: UseCase) {
        state.value = Space(useCase)
    }

    override fun createSpace(useCase: UseCase, name: String) {
        state.value = Space(useCase, name)
    }
}
