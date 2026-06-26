package com.arenacun.kuodra.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Preferencias de usuario. Hoy solo el tema; futuro candidato a DataStore. */
interface PreferencesRepository {
    val darkTheme: StateFlow<Boolean>
    fun toggleTheme()
}
