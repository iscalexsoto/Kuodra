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

    private class FakeSpaceRepository : SpaceRepository {
        override val activeSpace: StateFlow<Space> = MutableStateFlow(Space(UseCase.Gastos))
        override fun selectUseCase(useCase: UseCase) = Unit
        override fun createSpace(useCase: UseCase, name: String) = Unit
        override suspend fun isConfigured(): Boolean = true
    }

    private class FakeMovementRepository(initial: List<Movement>) : MovementRepository {
        private val movements = MutableStateFlow(initial)
        override fun movements(useCase: UseCase): Flow<List<Movement>> = movements.asStateFlow()
        override suspend fun movement(useCase: UseCase, id: String): Movement? = movements.value.find { it.id == id }
        override suspend fun add(useCase: UseCase, movement: Movement) { movements.update { it + movement } }
        override suspend fun update(useCase: UseCase, movement: Movement) {
            movements.update { list -> list.map { if (it.id == movement.id) movement else it } }
        }
        override suspend fun delete(useCase: UseCase, id: String) {
            movements.update { list -> list.filterNot { it.id == id } }
        }
    }

    private class FakeSummaryRepository : SummaryRepository {
        override fun people(useCase: UseCase): List<Person> = emptyList()
        override fun categories(): List<Category> = emptyList()
    }

    private class FakeSettingsRepository : SettingsRepository {
        override fun settings(useCase: UseCase): StateFlow<SpaceSettings> =
            MutableStateFlow(SpaceSettings(name = "", members = emptyList(), budget = null, fund = null, reminderEnabled = false))
        override fun update(useCase: UseCase, settings: SpaceSettings) = Unit
        override fun history(useCase: UseCase): List<SettlementRecord> = emptyList()
        override fun historyEntry(useCase: UseCase, id: String): SettlementRecord? = null
    }

    private class FakeSnapshotRepository : SnapshotRepository {
        override val snapshots: StateFlow<List<PeriodSnapshot>> = MutableStateFlow(emptyList())
        override suspend fun add(snapshot: PeriodSnapshot) = Unit
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
        )

        // Activa la suscripción (SharingStarted.WhileSubscribed).
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.movements.size)

        movementRepository.delete(UseCase.Gastos, "a")
        advanceUntilIdle()

        assertEquals(listOf("b"), viewModel.uiState.value.movements.map { it.id })
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
        )

        viewModel.onOpenMenu()
        assertEquals(DashboardSheet.Menu, viewModel.overlay.value.sheet)

        viewModel.onShare()
        assertEquals(DashboardSheet.Share, viewModel.overlay.value.sheet)
        viewModel.onShareConfirm()
        assertEquals(DashboardSheet.Shared, viewModel.overlay.value.sheet)

        viewModel.onClosePeriod()
        assertEquals(DashboardSheet.PCloseConfirm, viewModel.overlay.value.sheet)
        viewModel.onClosePeriodConfirm()
        advanceUntilIdle()
        assertEquals(DashboardSheet.PClosed, viewModel.overlay.value.sheet)

        viewModel.onCloseSheet()
        assertEquals(DashboardSheet.None, viewModel.overlay.value.sheet)
    }
}
