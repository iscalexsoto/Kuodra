package com.arenacun.kuodra.presentation.feature.dashboard

import com.arenacun.kuodra.MainDispatcherRule
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.MovementRepository
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
        Movement(id, id, "meta", "$10", "Ca", "Cat", AvatarTone.Tint, "hoy")

    private class FakeSpaceRepository : SpaceRepository {
        override val activeSpace: StateFlow<Space> = MutableStateFlow(Space(UseCase.Gastos))
        override fun selectUseCase(useCase: UseCase) = Unit
        override fun createSpace(useCase: UseCase, name: String) = Unit
        override suspend fun isConfigured(): Boolean = true
    }

    private class FakeMovementRepository(initial: List<Movement>) : MovementRepository {
        private val movements = MutableStateFlow(initial)
        override fun movements(useCase: UseCase): Flow<List<Movement>> = movements.asStateFlow()
        override fun movement(useCase: UseCase, id: String): Movement? = movements.value.find { it.id == id }
        override fun delete(id: String) = movements.update { list -> list.filterNot { it.id == id } }
        override fun add(useCase: UseCase, movement: Movement) = movements.update { it + movement }
    }

    private class FakeSummaryRepository : SummaryRepository {
        override fun people(useCase: UseCase): List<Person> = emptyList()
        override fun categories(): List<Category> = emptyList()
    }

    @Test
    fun `deleting a movement removes it from the dashboard state`() = runTest {
        val movementRepository = FakeMovementRepository(listOf(movement("a"), movement("b")))
        val viewModel = DashboardViewModel(
            spaceRepository = FakeSpaceRepository(),
            movementRepository = movementRepository,
            summaryRepository = FakeSummaryRepository(),
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
    fun `menu actions open the matching sheet`() = runTest {
        val viewModel = DashboardViewModel(
            spaceRepository = FakeSpaceRepository(),
            movementRepository = FakeMovementRepository(emptyList()),
            summaryRepository = FakeSummaryRepository(),
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
        assertEquals(DashboardSheet.PClosed, viewModel.overlay.value.sheet)

        viewModel.onCloseSheet()
        assertEquals(DashboardSheet.None, viewModel.overlay.value.sheet)
    }
}
