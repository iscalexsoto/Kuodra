package com.arenacun.kuodra.presentation.feature.dashboard

import com.arenacun.kuodra.MainDispatcherRule
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.PeriodSnapshot
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SnapshotRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun movement(id: String) =
        Movement(id, Money.ofMajor(10.0), "otro", id)

    private class FakeSpaceRepository(useCase: UseCase = UseCase.Gastos) : SpaceRepository {
        private val active = if (useCase == UseCase.Personal) Space.PERSONAL
            else Space(id = "s1", useCase = UseCase.Gastos, name = "Casa")
        override val activeSpace: StateFlow<Space> = MutableStateFlow(active)
        override val spaces: Flow<List<Space>> = MutableStateFlow(emptyList())
        override fun selectPersonal() = Unit
        override fun selectSpace(id: String) = Unit
        override suspend fun createSpace(name: String): Space = active
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
        override suspend fun add(movement: Movement) { movements.update { it + movement } }
        override suspend fun update(movement: Movement) {
            movements.update { list -> list.map { if (it.id == movement.id) movement else it } }
        }
        override suspend fun delete(id: String) {
            movements.update { list -> list.filterNot { it.id == id } }
        }
    }

    private class FakeSummaryRepository : SummaryRepository {
        override fun categories(): List<Category> = emptyList()
    }

    private class FakePersonRepository : com.arenacun.kuodra.domain.repository.PersonRepository {
        override fun persons(spaceId: String): Flow<List<com.arenacun.kuodra.domain.model.SpacePerson>> =
            MutableStateFlow(emptyList())
        override suspend fun add(spaceId: String, person: com.arenacun.kuodra.domain.model.SpacePerson) = Unit
        override suspend fun update(spaceId: String, person: com.arenacun.kuodra.domain.model.SpacePerson) = Unit
        override suspend fun delete(id: String) = Unit
    }

    private class FakeSettingsRepository(
        private val settings: SpaceSettings =
            SpaceSettings(name = "", members = emptyList(), budget = null, reminderEnabled = false),
    ) : SettingsRepository {
        override fun settings(): StateFlow<SpaceSettings> = MutableStateFlow(settings)
        override fun updateBudget(budget: com.arenacun.kuodra.domain.model.BudgetConfig) = Unit
        override fun history(): List<SettlementRecord> = emptyList()
        override fun historyEntry(id: String): SettlementRecord? = null
    }

    private class FakeSnapshotRepository : SnapshotRepository {
        override val snapshots: StateFlow<List<PeriodSnapshot>> = MutableStateFlow(emptyList())
        override suspend fun add(snapshot: PeriodSnapshot) = Unit
    }

    private class FakeSettlementRepository : com.arenacun.kuodra.domain.repository.SettlementRepository {
        override fun settlements(spaceId: String): kotlinx.coroutines.flow.Flow<List<com.arenacun.kuodra.domain.model.Settlement>> =
            MutableStateFlow(emptyList())
        override suspend fun close(settlement: com.arenacun.kuodra.domain.model.Settlement, movementIds: List<String>, paymentIds: List<String>) = Unit
        override suspend fun record(payment: com.arenacun.kuodra.domain.model.Settlement) = Unit
    }

    @Test
    fun `deleting a movement removes it from the dashboard state`() = runTest {
        val movementRepository = FakeMovementRepository(listOf(movement("a"), movement("b")))
        val viewModel = DashboardViewModel(
            spaceRepository = FakeSpaceRepository(),
            movementRepository = movementRepository,
            summaryRepository = FakeSummaryRepository(),
            settingsRepository = FakeSettingsRepository(),
            snapshotRepository = FakeSnapshotRepository(),
            personRepository = FakePersonRepository(),
            settlementRepository = FakeSettlementRepository(),
        )

        // Activa la suscripción (SharingStarted.WhileSubscribed).
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.movements.size)

        movementRepository.delete("a")
        advanceUntilIdle()

        assertEquals(listOf("b"), viewModel.uiState.value.movements.map { it.id })
        collectJob.cancel()
    }

    @Test
    fun `pending returns total sums all pending and mark-all stamps the current percent`() = runTest {
        val budget = com.arenacun.kuodra.domain.model.BudgetConfig.Default.copy(returnPercent = 50)
        val settings = SpaceSettings(name = "", members = emptyList(), budget = budget, reminderEnabled = false)
        val movements = FakeMovementRepository(
            listOf(
                Movement("a", Money.ofMajor(100.0), "otro", "a", returnStatus = com.arenacun.kuodra.domain.model.ReturnStatus.Pending),
                Movement("b", Money.ofMajor(40.0), "otro", "b", returnStatus = com.arenacun.kuodra.domain.model.ReturnStatus.Pending),
                Movement("c", Money.ofMajor(999.0), "otro", "c"),
            ),
        )
        val viewModel = DashboardViewModel(
            spaceRepository = FakeSpaceRepository(UseCase.Personal),
            movementRepository = movements,
            summaryRepository = FakeSummaryRepository(),
            settingsRepository = FakeSettingsRepository(settings),
            snapshotRepository = FakeSnapshotRepository(),
            personRepository = FakePersonRepository(),
            settlementRepository = FakeSettlementRepository(),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        // (100 + 40) * 50% = 70
        assertEquals("$70", viewModel.uiState.value.pendingReturns?.totalLabel)

        viewModel.onMarkAllReturned()
        assertEquals(DashboardSheet.ReturnAllConfirm, viewModel.overlay.value.sheet)
        viewModel.onMarkAllReturnedConfirm()
        advanceUntilIdle()

        assertEquals(DashboardSheet.ReturnAllDone, viewModel.overlay.value.sheet)
        assertEquals(null, viewModel.uiState.value.pendingReturns)
        val a = viewModel.uiState.value.movements.first { it.id == "a" }
        assertEquals(com.arenacun.kuodra.domain.model.ReturnStatus.Returned, a.returnStatus)
        collectJob.cancel()
    }

    @Test
    fun `menu actions open the matching sheet`() = runTest {
        val viewModel = DashboardViewModel(
            spaceRepository = FakeSpaceRepository(),
            movementRepository = FakeMovementRepository(emptyList()),
            summaryRepository = FakeSummaryRepository(),
            settingsRepository = FakeSettingsRepository(),
            snapshotRepository = FakeSnapshotRepository(),
            personRepository = FakePersonRepository(),
            settlementRepository = FakeSettlementRepository(),
        )

        viewModel.onOpenMenu()
        assertEquals(DashboardSheet.Menu, viewModel.overlay.value.sheet)

        // Compartir resumen: emite el texto por el flujo `share` y cierra el menú (share nativo).
        val shared = mutableListOf<String>()
        val shareJob = launch { viewModel.share.collect { shared += it } }
        viewModel.onShare()
        advanceUntilIdle()
        assertEquals(DashboardSheet.None, viewModel.overlay.value.sheet)
        assertEquals(1, shared.size)
        shareJob.cancel()

        viewModel.onClosePeriod()
        assertEquals(DashboardSheet.PCloseConfirm, viewModel.overlay.value.sheet)
        viewModel.onClosePeriodConfirm()
        advanceUntilIdle()
        assertEquals(DashboardSheet.PClosed, viewModel.overlay.value.sheet)

        viewModel.onCloseSheet()
        assertEquals(DashboardSheet.None, viewModel.overlay.value.sheet)
    }
}
