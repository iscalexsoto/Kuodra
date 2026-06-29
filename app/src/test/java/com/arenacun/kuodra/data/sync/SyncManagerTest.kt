package com.arenacun.kuodra.data.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.BudgetDao
import com.arenacun.kuodra.data.local.db.BudgetEntity
import com.arenacun.kuodra.data.local.db.CategoryDao
import com.arenacun.kuodra.data.local.db.CategoryEntity
import com.arenacun.kuodra.data.local.db.MovementDao
import com.arenacun.kuodra.data.local.db.MovementEntity
import com.arenacun.kuodra.data.local.db.PeriodSnapshotDao
import com.arenacun.kuodra.data.local.db.PeriodSnapshotEntity
import com.arenacun.kuodra.data.remote.BudgetApi
import com.arenacun.kuodra.data.remote.CategoryApi
import com.arenacun.kuodra.data.remote.MovementApi
import com.arenacun.kuodra.data.remote.PeriodSnapshotApi
import com.arenacun.kuodra.data.remote.dto.BudgetDto
import com.arenacun.kuodra.data.remote.dto.CategoryDto
import com.arenacun.kuodra.data.remote.dto.MovementDto
import com.arenacun.kuodra.data.remote.dto.PeriodSnapshotDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class SyncManagerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class Env(val manager: SyncManager, val session: SessionStore, val cursors: SyncCursorStore)

    private fun newEnv(
        movementApi: MovementApi,
        movementDao: MovementDao,
        categoryApi: CategoryApi = EmptyCategoryApi(),
        categoryDao: CategoryDao = EmptyCategoryDao(),
    ): Env {
        val dataStore = PreferenceDataStoreFactory.create { tmp.newFile("sync.preferences_pb") }
        val session = SessionStore(dataStore)
        val cursors = SyncCursorStore(dataStore)
        val manager = SyncManager(
            movementApi, categoryApi, EmptyBudgetApi(), EmptyPeriodSnapshotApi(),
            movementDao, categoryDao, EmptyBudgetDao(), EmptyPeriodSnapshotDao(),
            session, cursors,
        )
        return Env(manager, session, cursors)
    }

    private suspend fun SessionStore.signIn() = save("tok", "u1", "u1@x.com", "U")

    private fun entity(id: String, dirty: Boolean, remoteUpdated: String = "", deleted: Boolean = false) =
        MovementEntity(
            id = id, owner = "u1", amountCents = 1000, categoryId = "uncategorized", title = id,
            note = "", date = LocalDate.of(2026, 6, 20), payer = null, splitNames = emptyList(),
            updatedAt = 0, deleted = deleted, dirty = dirty, remoteUpdated = remoteUpdated,
        )

    private fun dto(id: String, updated: String, deleted: Boolean = false) =
        MovementDto(id = id, owner = "u1", amount = 2000, category = "uncategorized", title = id,
            date = "2026-06-21", deleted = deleted, updated = updated)

    @Test
    fun `push uploads dirty rows and marks them synced`() = runTest {
        val dao = FakeMovementDao(mutableListOf(entity("m1", dirty = true)))
        val api = FakeMovementApi(createdUpdated = "2026-06-20 10:00:00.000Z")
        val env = newEnv(api, dao)
        env.session.signIn()

        val result = env.manager.sync()

        assertTrue(result.isSuccess)
        assertEquals(listOf("m1"), api.created.map { it.id })
        val row = dao.rows.first { it.id == "m1" }
        assertFalse(row.dirty)
        assertEquals("2026-06-20 10:00:00.000Z", row.remoteUpdated)
    }

    @Test
    fun `pull inserts remote rows and advances cursor`() = runTest {
        val dao = FakeMovementDao(mutableListOf())
        val api = FakeMovementApi(listResult = listOf(dto("m2", "2026-06-21 09:00:00.000Z")))
        val env = newEnv(api, dao)
        env.session.signIn()

        env.manager.sync()

        assertEquals(listOf("m2"), dao.rows.map { it.id })
        assertEquals("2026-06-21 09:00:00.000Z", env.cursors.get("movements"))
    }

    @Test
    fun `pull does not overwrite a locally dirty row`() = runTest {
        val dao = FakeMovementDao(mutableListOf(entity("m3", dirty = true, remoteUpdated = "old")))
        val api = FakeMovementApi(
            createdUpdated = "2026-06-20 10:00:00.000Z",
            listResult = listOf(dto("m3", "2026-06-21 09:00:00.000Z").copy(title = "remote")),
        )
        val env = newEnv(api, dao)
        env.session.signIn()

        env.manager.sync()

        // Tras el push, la fila quedó limpia; el pull la trae con el dato remoto (ya no dirty).
        val row = dao.rows.first { it.id == "m3" }
        assertEquals("remote", row.title)
        assertFalse(row.dirty)
    }

    @Test
    fun `no session is a no-op success`() = runTest {
        val dao = FakeMovementDao(mutableListOf(entity("m1", dirty = true)))
        val api = FakeMovementApi()
        val env = newEnv(api, dao)

        val result = env.manager.sync()

        assertTrue(result.isSuccess)
        assertTrue(api.created.isEmpty())
    }

    // --- Fakes ---

    private class FakeMovementApi(
        var createdUpdated: String = "2026-01-01 00:00:00.000Z",
        var listResult: List<MovementDto> = emptyList(),
    ) : MovementApi {
        val created = mutableListOf<MovementDto>()
        val updated = mutableListOf<MovementDto>()
        override suspend fun list(since: String, token: String): List<MovementDto> = listResult
        override suspend fun create(dto: MovementDto, token: String): MovementDto {
            created += dto; return dto.copy(updated = createdUpdated)
        }
        override suspend fun update(dto: MovementDto, token: String): MovementDto {
            updated += dto; return dto.copy(updated = createdUpdated)
        }
    }

    private class FakeMovementDao(val rows: MutableList<MovementEntity>) : MovementDao {
        override fun observe(owner: String): Flow<List<MovementEntity>> = flowOf(rows.toList())
        override suspend fun find(id: String): MovementEntity? = rows.find { it.id == id }
        override suspend fun dirtyRows(owner: String): List<MovementEntity> = rows.filter { it.dirty }
        override suspend fun markSynced(id: String, remoteUpdated: String) {
            rows.replaceAll { if (it.id == id) it.copy(dirty = false, remoteUpdated = remoteUpdated) else it }
        }
        override suspend fun upsert(movement: MovementEntity) {
            rows.removeAll { it.id == movement.id }; rows += movement
        }
        override suspend fun softDelete(id: String, updatedAt: Long) {
            rows.replaceAll { if (it.id == id) it.copy(deleted = true, dirty = true) else it }
        }
    }

    private class EmptyCategoryApi : CategoryApi {
        override suspend fun list(since: String, token: String): List<CategoryDto> = emptyList()
        override suspend fun create(dto: CategoryDto, token: String): CategoryDto = dto
        override suspend fun update(dto: CategoryDto, token: String): CategoryDto = dto
    }
    private class EmptyCategoryDao : CategoryDao {
        override fun observe(owner: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun count(owner: String): Int = 0
        override suspend fun find(id: String): CategoryEntity? = null
        override suspend fun dirtyRows(owner: String): List<CategoryEntity> = emptyList()
        override suspend fun markSynced(id: String, remoteUpdated: String) = Unit
        override suspend fun upsert(category: CategoryEntity) = Unit
        override suspend fun upsertAll(categories: List<CategoryEntity>) = Unit
        override suspend fun softDelete(id: String, updatedAt: Long) = Unit
    }
    private class EmptyBudgetApi : BudgetApi {
        override suspend fun list(since: String, token: String): List<BudgetDto> = emptyList()
        override suspend fun create(dto: BudgetDto, token: String): BudgetDto = dto
        override suspend fun update(dto: BudgetDto, token: String): BudgetDto = dto
    }
    private class EmptyBudgetDao : BudgetDao {
        override fun observe(owner: String): Flow<BudgetEntity?> = flowOf(null)
        override suspend fun dirtyRows(owner: String): List<BudgetEntity> = emptyList()
        override suspend fun markSynced(owner: String, remoteUpdated: String) = Unit
        override suspend fun upsert(budget: BudgetEntity) = Unit
    }
    private class EmptyPeriodSnapshotApi : PeriodSnapshotApi {
        override suspend fun list(since: String, token: String): List<PeriodSnapshotDto> = emptyList()
        override suspend fun create(dto: PeriodSnapshotDto, token: String): PeriodSnapshotDto = dto
        override suspend fun update(dto: PeriodSnapshotDto, token: String): PeriodSnapshotDto = dto
    }
    private class EmptyPeriodSnapshotDao : PeriodSnapshotDao {
        override fun observe(owner: String): Flow<List<PeriodSnapshotEntity>> = flowOf(emptyList())
        override suspend fun find(id: String): PeriodSnapshotEntity? = null
        override suspend fun dirtyRows(owner: String): List<PeriodSnapshotEntity> = emptyList()
        override suspend fun markSynced(id: String, remoteUpdated: String) = Unit
        override suspend fun upsert(snapshot: PeriodSnapshotEntity) = Unit
    }
}
