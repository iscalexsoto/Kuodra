package com.arenacun.kuodra.presentation.feature.settle

import com.arenacun.kuodra.MainDispatcherRule
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SplitShare
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SettlementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSpaceRepository : SpaceRepository {
        override val activeSpace: StateFlow<Space> =
            MutableStateFlow(Space(id = "s1", useCase = UseCase.Gastos, name = "Casa"))
        override val spaces: Flow<List<Space>> = MutableStateFlow(emptyList())
        override fun selectPersonal() = Unit
        override fun selectSpace(id: String) = Unit
        override suspend fun createSpace(name: String): Space = activeSpace.value
        override suspend fun rename(id: String, name: String) = Unit
        override suspend fun setReminder(id: String, enabled: Boolean) = Unit
        override suspend fun archive(id: String) = Unit
        override suspend fun unarchive(id: String) = Unit
        override suspend fun isConfigured(): Boolean = true
    }

    private class FakeMovementRepository(initial: List<Movement>) : MovementRepository {
        private val movements = MutableStateFlow(initial)
        override fun movements(spaceId: String): Flow<List<Movement>> = movements.asStateFlow()
        override suspend fun movement(id: String): Movement? = movements.value.find { it.id == id }
        override suspend fun add(movement: Movement) = Unit
        override suspend fun update(movement: Movement) = Unit
        override suspend fun delete(id: String) = Unit
        fun stamp(ids: List<String>, settlementId: String) {
            movements.update { list -> list.map { if (it.id in ids) it.copy(settlementId = settlementId) else it } }
        }
    }

    private class FakePersonRepository(private val people: List<SpacePerson>) : PersonRepository {
        override fun persons(spaceId: String): Flow<List<SpacePerson>> = MutableStateFlow(people)
        override suspend fun add(spaceId: String, person: SpacePerson) = Unit
        override suspend fun update(spaceId: String, person: SpacePerson) = Unit
        override suspend fun delete(id: String) = Unit
    }

    private class RecordingSettlementRepository(private val movements: FakeMovementRepository) : SettlementRepository {
        val closed = mutableListOf<Settlement>()
        override fun settlements(spaceId: String): Flow<List<Settlement>> = MutableStateFlow(closed)
        override suspend fun close(settlement: Settlement, movementIds: List<String>) {
            closed += settlement
            movements.stamp(movementIds, settlement.id) // estampa como lo haría el impl real
        }
    }

    private fun expense() = Movement(
        id = "m1", amount = Money(90000), categoryId = "otro", title = "Cena", spaceId = "s1",
        payers = listOf(PayerShare(PersonRef.ME, Money(90000))),
        splits = listOf(SplitShare(PersonRef.ME, Money(30000)), SplitShare("a", Money(60000))),
    )

    @Test
    fun `register closes the settlement and stamps the movements so balances reset`() = runTest {
        val movements = FakeMovementRepository(listOf(expense()))
        val settlements = RecordingSettlementRepository(movements)
        val vm = SettleViewModel(
            FakeSpaceRepository(), movements,
            FakePersonRepository(listOf(SpacePerson("a", "Andrea", "+521"))), settlements,
        )
        val job = launch { vm.uiState.collect {} }
        advanceUntilIdle()
        // Andrea debe 600 (te debe); tú neto +600.
        assertEquals("+$600", vm.uiState.value.people.single().person.amount)

        vm.onRegister()
        advanceUntilIdle()

        assertEquals(1, settlements.closed.size)
        assertEquals(Money(90000), settlements.closed.single().total)
        // Tras estampar, no quedan balances vivos.
        assertTrue(vm.uiState.value.people.isEmpty())
        job.cancel()
    }
}
