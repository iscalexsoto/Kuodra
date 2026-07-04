package com.arenacun.kuodra.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.arenacun.kuodra.data.local.KuodraSeedSource
import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.MovementDao
import com.arenacun.kuodra.data.local.db.MovementEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Rama seed (Gastos/Caja) de [MovementRepositoryImpl]: `update` debe reemplazar por id
 * (no duplicar), tanto para movimientos añadidos como para los base del seed.
 */
class MovementRepositoryImplTest {

    private class StubMovementDao : MovementDao {
        override fun observe(owner: String): Flow<List<MovementEntity>> = flowOf(emptyList())
        override suspend fun find(id: String): MovementEntity? = null
        override suspend fun dirtyRows(owner: String): List<MovementEntity> = emptyList()
        override suspend fun markSynced(id: String, remoteUpdated: String) = Unit
        override suspend fun upsert(movement: MovementEntity) = Unit
        override suspend fun softDelete(id: String, updatedAt: Long) = Unit
    }

    private val emptyDataStore = object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(emptyPreferences())
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = emptyPreferences()
    }

    private fun repository(seed: KuodraSeedSource = KuodraSeedSource()) = MovementRepositoryImpl(
        seed = seed,
        dao = StubMovementDao(),
        sessionStore = SessionStore(emptyDataStore),
        syncTrigger = SyncTrigger.NoOp,
    )

    @Test
    fun `update of a base seed movement replaces it without duplicating`() = runTest {
        val seed = KuodraSeedSource()
        val repo = repository(seed)
        val base = seed.baseMovements(UseCase.Gastos).first()

        repo.update(UseCase.Gastos, base.copy(title = "Editado", amount = Money(99900)))

        val all = repo.movements(UseCase.Gastos).first()
        val matches = all.filter { it.id == base.id }
        assertEquals(1, matches.size)
        assertEquals("Editado", matches.single().title)
        assertEquals(99900L, matches.single().amount.cents)
    }

    @Test
    fun `update of an added movement replaces it without duplicating`() = runTest {
        val repo = repository()
        val movement = Movement(id = "nuevo", amount = Money(1000), categoryId = "otro", title = "Café")
        repo.add(UseCase.Gastos, movement)

        repo.update(UseCase.Gastos, movement.copy(title = "Café con pan"))

        val all = repo.movements(UseCase.Gastos).first()
        val matches = all.filter { it.id == "nuevo" }
        assertEquals(1, matches.size)
        assertEquals("Café con pan", matches.single().title)
    }

    @Test
    fun `movement returns the edited version`() = runTest {
        val seed = KuodraSeedSource()
        val repo = repository(seed)
        val base = seed.baseMovements(UseCase.Caja).first()

        repo.update(UseCase.Caja, base.copy(title = "Corregido"))

        assertEquals("Corregido", repo.movement(UseCase.Caja, base.id)?.title)
    }

    @Test
    fun `delete after edit still removes the movement`() = runTest {
        val seed = KuodraSeedSource()
        val repo = repository(seed)
        val base = seed.baseMovements(UseCase.Gastos).first()
        repo.update(UseCase.Gastos, base.copy(title = "Editado"))

        repo.delete(UseCase.Gastos, base.id)

        assertNull(repo.movements(UseCase.Gastos).first().find { it.id == base.id })
    }
}
