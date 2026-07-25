package com.arenacun.kuodra.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.SpaceDao
import com.arenacun.kuodra.data.local.db.SpaceEntity
import com.arenacun.kuodra.data.mapper.toDomain
import com.arenacun.kuodra.data.mapper.toRuleJson
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.SplitRule
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.newId
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Espacio activo + espacios de Gastos. La lista de Gastos vive en Room (offline + sync); el
 * puntero al espacio activo (caso de uso + id) vive en DataStore. Si el espacio activo se archiva o
 * desaparece (pull remoto), cae a Personal.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpaceRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val dao: SpaceDao,
    private val sessionStore: SessionStore,
    private val syncTrigger: SyncTrigger,
) : SpaceRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val spaces: Flow<List<Space>> =
        sessionStore.sessionFlow.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else dao.observe(session.userId).map { rows -> rows.map { it.toDomain() } }
        }

    override val activeSpace: StateFlow<Space> =
        combine(dataStore.data, spaces) { prefs, spaceList ->
            val useCase = prefs[USE_CASE]?.let { runCatching { UseCase.valueOf(it) }.getOrNull() }
            if (useCase != UseCase.Gastos) {
                Space.PERSONAL
            } else {
                val id = prefs[SPACE_ID].orEmpty()
                spaceList.firstOrNull { it.id == id && !it.archived } ?: Space.PERSONAL
            }
        }.stateIn(scope, SharingStarted.Eagerly, Space.PERSONAL)

    override fun selectPersonal() {
        scope.launch {
            dataStore.edit {
                it[USE_CASE] = UseCase.Personal.name
                it.remove(SPACE_ID)
            }
        }
    }

    override fun selectSpace(id: String) {
        scope.launch {
            dataStore.edit {
                it[USE_CASE] = UseCase.Gastos.name
                it[SPACE_ID] = id
            }
        }
    }

    override suspend fun createSpace(name: String): Space {
        val owner = sessionStore.userId() ?: return Space.PERSONAL
        val space = Space(id = newId(), useCase = UseCase.Gastos, name = name)
        dao.upsert(
            SpaceEntity(
                id = space.id, owner = owner, name = name, archived = false,
                reminderEnabled = true, updatedAt = now(), deleted = false, dirty = true,
            ),
        )
        dataStore.edit {
            it[USE_CASE] = UseCase.Gastos.name
            it[SPACE_ID] = space.id
        }
        syncTrigger.requestSync()
        return space
    }

    override suspend fun rename(id: String, name: String) = mutate(id) { it.copy(name = name) }
    override suspend fun setReminder(id: String, enabled: Boolean) = mutate(id) { it.copy(reminderEnabled = enabled) }
    override suspend fun archive(id: String) {
        mutate(id) { it.copy(archived = true) }
        // Si era el activo, vuelve a Personal.
        if (dataStore.data.first()[SPACE_ID] == id) selectPersonal()
    }
    override suspend fun unarchive(id: String) = mutate(id) { it.copy(archived = false) }
    override suspend fun setSplitRule(id: String, rule: SplitRule) =
        mutate(id) { it.copy(splitRuleJson = rule.toRuleJson()) }

    override suspend fun isConfigured(): Boolean = dataStore.data.first()[USE_CASE] != null

    private suspend fun mutate(id: String, transform: (SpaceEntity) -> SpaceEntity) {
        val current = dao.find(id) ?: return
        dao.upsert(transform(current).copy(updatedAt = now(), dirty = true))
        syncTrigger.requestSync()
    }

    private fun now() = System.currentTimeMillis()

    private companion object {
        val USE_CASE = stringPreferencesKey("active_use_case")
        val SPACE_ID = stringPreferencesKey("active_space_id")
    }
}
