package com.arenacun.kuodra.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Espacio activo persistido en DataStore (sobrevive reinicios). */
class SpaceRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SpaceRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val activeSpace: StateFlow<Space> = dataStore.data
        .map { prefs ->
            val useCase = prefs[USE_CASE]?.let { runCatching { UseCase.valueOf(it) }.getOrNull() }
            Space(useCase ?: UseCase.Personal, prefs[SPACE_NAME].orEmpty())
        }
        .stateIn(scope, SharingStarted.Eagerly, Space(UseCase.Personal))

    override fun selectUseCase(useCase: UseCase) {
        scope.launch {
            dataStore.edit {
                it[USE_CASE] = useCase.name
                it.remove(SPACE_NAME)
            }
        }
    }

    override fun createSpace(useCase: UseCase, name: String) {
        scope.launch {
            dataStore.edit {
                it[USE_CASE] = useCase.name
                it[SPACE_NAME] = name
            }
        }
    }

    override suspend fun isConfigured(): Boolean = dataStore.data.first()[USE_CASE] != null

    private companion object {
        val USE_CASE = stringPreferencesKey("active_use_case")
        val SPACE_NAME = stringPreferencesKey("active_space_name")
    }
}
