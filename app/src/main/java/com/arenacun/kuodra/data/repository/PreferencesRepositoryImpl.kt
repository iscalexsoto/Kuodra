package com.arenacun.kuodra.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.arenacun.kuodra.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Preferencias de usuario persistidas en DataStore. */
class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val darkTheme: StateFlow<Boolean> = dataStore.data
        .map { it[DARK_THEME] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override fun toggleTheme() {
        scope.launch {
            dataStore.edit { it[DARK_THEME] = !(it[DARK_THEME] ?: false) }
        }
    }

    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}
