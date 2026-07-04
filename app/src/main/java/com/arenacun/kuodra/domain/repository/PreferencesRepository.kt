package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** Preferencias de usuario persistidas en DataStore. Hoy solo el tema. */
interface PreferencesRepository {
    val themeMode: StateFlow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)
}
