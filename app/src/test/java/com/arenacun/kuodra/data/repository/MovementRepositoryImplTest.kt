package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.TestPrefsRule
import com.arenacun.kuodra.data.local.db.MovementDao
import com.arenacun.kuodra.data.local.db.MovementEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * [MovementRepositoryImpl] sobre Room: `add`/`update` hacen upsert `dirty` filtrado por espacio,
 * y `delete` marca el tombstone. Fake DAO en memoria + `SessionStore` real sobre un DataStore temporal.
 */
class MovementRepositoryImplTest {

    @get:Rule
    val prefs = TestPrefsRule()

    private class FakeMovementDao(val rows: MutableList<MovementEntity> = mutableListOf()) : MovementDao {
        override fun observe(owner: String): Flow<List<MovementEntity>> = flowOf(rows.filter { !it.deleted })
        override fun observe(owner: String, space: String): Flow<List<MovementEntity>> =
            flowOf(rows.filter { it.owner == owner && it.space == space && !it.deleted })
        override suspend fun find(id: String): MovementEntity? = rows.find { it.id == id }
        override suspend fun dirtyRows(owner: String): List<MovementEntity> = rows.filter { it.dirty }
        override suspend fun markSynced(id: String, remoteUpdated: String) = Unit
        override suspend fun upsert(movement: MovementEntity) {
            rows.removeAll { it.id == movement.id }; rows += movement
        }
        override suspend fun softDelete(id: String, updatedAt: Long) {
            rows.replaceAll { if (it.id == id) it.copy(deleted = true, dirty = true) else it }
        }
        override suspend fun stampSettlement(ids: List<String>, settlementId: String, updatedAt: Long) {
            rows.replaceAll { if (it.id in ids) it.copy(settlementId = settlementId, dirty = true) else it }
        }
    }

    private suspend fun repositoryWithSession(dao: FakeMovementDao): MovementRepositoryImpl {
        val session = prefs.sessionStore("mov.preferences_pb")
        session.save("tok", "u1", "u1@x.com", "U")
        return MovementRepositoryImpl(dao, session, SyncTrigger.NoOp)
    }

    private fun movement(id: String, spaceId: String, title: String = id) =
        Movement(id = id, amount = Money(1000), categoryId = "otro", title = title, spaceId = spaceId)

    @Test
    fun `add upserts a dirty row filtered by space`() = runTest {
        val dao = FakeMovementDao()
        val repo = repositoryWithSession(dao)

        repo.add(movement("g1", spaceId = "s1"))

        assertEquals(listOf("g1"), repo.movements("s1").first().map { it.id })
        assertEquals(emptyList<String>(), repo.movements("").first().map { it.id })
        assertEquals(true, dao.rows.first { it.id == "g1" }.dirty)
    }

    @Test
    fun `update replaces the row by id`() = runTest {
        val dao = FakeMovementDao()
        val repo = repositoryWithSession(dao)
        repo.add(movement("g1", spaceId = "s1"))

        repo.update(movement("g1", spaceId = "s1", title = "Editado"))

        val all = repo.movements("s1").first().filter { it.id == "g1" }
        assertEquals(1, all.size)
        assertEquals("Editado", all.single().title)
        assertEquals("Editado", repo.movement("g1")?.title)
    }

    @Test
    fun `delete marks the tombstone and drops it from the flow`() = runTest {
        val dao = FakeMovementDao()
        val repo = repositoryWithSession(dao)
        repo.add(movement("g1", spaceId = "s1"))

        repo.delete("g1")

        assertNull(repo.movements("s1").first().find { it.id == "g1" })
    }
}
