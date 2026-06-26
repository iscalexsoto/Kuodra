package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Implementación en memoria. Migrable a DataStore sin cambiar el contrato. */
class PreferencesRepositoryImpl : PreferencesRepository {

    private val dark = MutableStateFlow(false)
    override val darkTheme: StateFlow<Boolean> = dark.asStateFlow()

    override fun toggleTheme() = dark.update { !it }
}
