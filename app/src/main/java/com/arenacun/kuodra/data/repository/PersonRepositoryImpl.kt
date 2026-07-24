package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.PersonDao
import com.arenacun.kuodra.data.mapper.toDomain
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.repository.PersonRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Contactos de un espacio: Room como fuente de verdad, escrituras `dirty` que dispara el sync. */
@OptIn(ExperimentalCoroutinesApi::class)
class PersonRepositoryImpl(
    private val dao: PersonDao,
    private val sessionStore: SessionStore,
    private val syncTrigger: SyncTrigger,
) : PersonRepository {

    override fun persons(spaceId: String): Flow<List<SpacePerson>> =
        sessionStore.sessionFlow.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else dao.observe(session.userId, spaceId).map { rows -> rows.map { it.toDomain() } }
        }

    override suspend fun add(spaceId: String, person: SpacePerson) = upsert(spaceId, person)
    override suspend fun update(spaceId: String, person: SpacePerson) = upsert(spaceId, person)

    override suspend fun delete(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
        syncTrigger.requestSync()
    }

    private suspend fun upsert(spaceId: String, person: SpacePerson) {
        val owner = sessionStore.userId() ?: return
        dao.upsert(person.toEntity(owner, spaceId, System.currentTimeMillis(), dirty = true))
        syncTrigger.requestSync()
    }
}
