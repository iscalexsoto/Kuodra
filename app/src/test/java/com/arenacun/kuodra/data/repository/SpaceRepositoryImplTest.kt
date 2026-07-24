package com.arenacun.kuodra.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.SpaceDao
import com.arenacun.kuodra.data.local.db.SpaceEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * [SpaceRepositoryImpl]: crear un espacio lo deja activo; archivar el activo cae a Personal.
 * Fake DAO reactivo en memoria + `SessionStore` real sobre un DataStore temporal.
 */
class SpaceRepositoryImplTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakeSpaceDao : SpaceDao {
        val rows = MutableStateFlow<List<SpaceEntity>>(emptyList())
        override fun observe(owner: String): Flow<List<SpaceEntity>> =
            rows.map { list -> list.filter { it.owner == owner && !it.deleted } }
        override suspend fun find(id: String): SpaceEntity? = rows.value.find { it.id == id }
        override suspend fun dirtyRows(owner: String): List<SpaceEntity> = rows.value.filter { it.dirty }
        override suspend fun markSynced(id: String, remoteUpdated: String) = Unit
        override suspend fun upsert(space: SpaceEntity) {
            rows.value = rows.value.filterNot { it.id == space.id } + space
        }
        override suspend fun softDelete(id: String, updatedAt: Long) {
            rows.value = rows.value.map { if (it.id == id) it.copy(deleted = true) else it }
        }
    }

    private suspend fun newRepo(dao: FakeSpaceDao): SpaceRepositoryImpl {
        val dataStore = PreferenceDataStoreFactory.create { tmp.newFile("space.preferences_pb") }
        val session = SessionStore(dataStore)
        session.save("tok", "u1", "u1@x.com", "U")
        return SpaceRepositoryImpl(dataStore, dao, session, SyncTrigger.NoOp)
    }

    @Test
    fun `createSpace inserts a dirty row and makes it active`() = runTest {
        val dao = FakeSpaceDao()
        val repo = newRepo(dao)

        val space = repo.createSpace("Casa Roma")

        assertEquals(1, dao.rows.value.size)
        assertEquals(true, dao.rows.value.first().dirty)
        val active = repo.activeSpace.first { it.id == space.id }
        assertEquals(UseCase.Gastos, active.useCase)
        assertEquals("Casa Roma", active.name)
    }

    @Test
    fun `archiving the active space falls back to Personal`() = runTest {
        val dao = FakeSpaceDao()
        val repo = newRepo(dao)
        val space = repo.createSpace("Viaje")
        repo.activeSpace.first { it.id == space.id }

        repo.archive(space.id)

        val active = repo.activeSpace.first { it.useCase == UseCase.Personal }
        assertEquals(UseCase.Personal, active.useCase)
    }
}
