package com.arenacun.kuodra.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.arenacun.kuodra.TestPrefsRule
import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.SpaceDao
import com.arenacun.kuodra.data.local.db.SpaceEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitRule
import com.arenacun.kuodra.domain.model.SplitRuleShare
import com.arenacun.kuodra.domain.model.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * [SpaceRepositoryImpl]: crear un espacio lo deja activo; archivar el activo cae a Personal.
 * Fake DAO reactivo en memoria + `SessionStore` real sobre un DataStore temporal.
 */
class SpaceRepositoryImplTest {

    @get:Rule
    val prefs = TestPrefsRule()

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

    private data class Env(
        val repo: SpaceRepositoryImpl,
        val dao: FakeSpaceDao,
        val dataStore: DataStore<Preferences>,
    )

    /**
     * El repo corre en `backgroundScope`: se cancela al terminar el cuerpo del test (antes de que la
     * regla borre los archivos) y sus corrutinas quedan bajo el reloj virtual, así que lo que el repo
     * lance sin esperar es observable en vez de una carrera contra `Dispatchers.IO`.
     */
    private suspend fun TestScope.newEnv(dao: FakeSpaceDao = FakeSpaceDao()): Env {
        val dataStore = prefs.dataStore("space.preferences_pb")
        val session = SessionStore(dataStore)
        session.save("tok", "u1", "u1@x.com", "U")
        return Env(
            SpaceRepositoryImpl(dataStore, dao, session, SyncTrigger.NoOp, scope = backgroundScope),
            dao,
            dataStore,
        )
    }

    @Test
    fun `createSpace inserts a dirty row and makes it active`() = runTest {
        val (repo, dao) = newEnv()

        val space = repo.createSpace("Casa Roma")

        assertEquals(1, dao.rows.value.size)
        assertEquals(true, dao.rows.value.first().dirty)
        val active = repo.activeSpace.first { it.id == space.id }
        assertEquals(UseCase.Gastos, active.useCase)
        assertEquals("Casa Roma", active.name)
    }

    @Test
    fun `archiving the active space falls back to Personal`() = runTest {
        val repo = newEnv().repo
        val space = repo.createSpace("Viaje")
        repo.activeSpace.first { it.id == space.id }

        repo.archive(space.id)

        val active = repo.activeSpace.first { it.useCase == UseCase.Personal }
        assertEquals(UseCase.Personal, active.useCase)
    }

    /**
     * Regresión: `archive` debe ESPERAR la escritura del puntero a Personal. Cuando la disparaba con
     * `selectPersonal()` (fire-and-forget en el scope del repo), al retornar no había escrito nada y
     * la corrutina suelta explotaba con `IOException` después de que la regla borrara el archivo; esa
     * excepción sin capturar la recogía el handler global y hacía fallar al SIGUIENTE `runTest` de la
     * suite (`UncaughtExceptionsBeforeTest`), con víctima variable según el orden de clases.
     */
    @Test
    fun `archiving the active space persists the fallback to Personal before returning`() = runTest {
        val env = newEnv()
        val space = env.repo.createSpace("Viaje")
        env.repo.activeSpace.first { it.id == space.id }

        env.repo.archive(space.id)

        // Sin advanceUntilIdle: al retornar, el puntero ya tiene que estar en disco.
        val stored = env.dataStore.data.first()
        assertEquals(UseCase.Personal.name, stored[stringPreferencesKey("active_use_case")])
        assertNull(stored[stringPreferencesKey("active_space_id")])
    }

    @Test
    fun `setSplitRule persists the rule and marks the row dirty`() = runTest {
        val (repo, dao) = newEnv()
        val space = repo.createSpace("Hermano")
        dao.rows.value = dao.rows.value.map { it.copy(dirty = false) }
        val rule = SplitRule(
            enabled = true,
            mode = SplitMode.Percent,
            shares = listOf(SplitRuleShare("me", 25), SplitRuleShare("p1", 75)),
            autoPersonalCopy = true,
        )

        repo.setSplitRule(space.id, rule)

        assertEquals(true, dao.rows.value.single().dirty)
        val active = repo.activeSpace.first { it.splitRule.enabled }
        assertEquals(rule, active.splitRule)
    }
}
