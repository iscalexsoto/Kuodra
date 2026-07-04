package com.arenacun.kuodra.presentation.feature.movement

import com.arenacun.kuodra.MainDispatcherRule
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.CategoryRepository
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import com.arenacun.kuodra.domain.scan.ParsedTicket
import com.arenacun.kuodra.domain.scan.ParsedTicketItem
import com.arenacun.kuodra.domain.scan.ScanSource
import com.arenacun.kuodra.domain.scan.TicketParseSource
import com.arenacun.kuodra.domain.scan.TicketScan
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddMovementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSpaceRepository : SpaceRepository {
        override val activeSpace: StateFlow<Space> = MutableStateFlow(Space(UseCase.Personal))
        override fun selectUseCase(useCase: UseCase) = Unit
        override fun createSpace(useCase: UseCase, name: String) = Unit
        override suspend fun isConfigured(): Boolean = true
    }

    private class FakeSummaryRepository : SummaryRepository {
        override fun people(useCase: UseCase): List<Person> = emptyList()
        override fun categories(): List<Category> = emptyList()
    }

    private class FakeCategoryRepository : CategoryRepository {
        override val categories: StateFlow<List<Category>> =
            MutableStateFlow(listOf(Category.Uncategorized))
        override suspend fun add(category: Category) = Unit
        override suspend fun update(category: Category) = Unit
        override suspend fun delete(id: String) = Unit
    }

    private class FakeMovementRepository(
        initial: List<Movement> = emptyList(),
    ) : MovementRepository {
        val added = mutableListOf<Movement>()
        val updated = mutableListOf<Movement>()
        private val movements = MutableStateFlow(initial)
        override fun movements(useCase: UseCase): Flow<List<Movement>> = movements.asStateFlow()
        override suspend fun movement(useCase: UseCase, id: String): Movement? =
            movements.value.find { it.id == id }
        override suspend fun add(useCase: UseCase, movement: Movement) {
            added += movement
            movements.update { it + movement }
        }
        override suspend fun update(useCase: UseCase, movement: Movement) {
            updated += movement
            movements.update { list -> list.map { if (it.id == movement.id) movement else it } }
        }
        override suspend fun delete(useCase: UseCase, id: String) = Unit
    }

    private fun viewModel(
        repository: FakeMovementRepository = FakeMovementRepository(),
        editId: String? = null,
    ) = AddMovementViewModel(
        editId = editId,
        spaceRepository = FakeSpaceRepository(),
        summaryRepository = FakeSummaryRepository(),
        categoryRepository = FakeCategoryRepository(),
        movementRepository = repository,
    )

    private fun scan(
        merchant: String? = "OXXO",
        total: Money? = Money(5950),
        date: LocalDate? = LocalDate.now().minusDays(2),
        items: List<ParsedTicketItem> = emptyList(),
    ) = TicketScan(
        rawText = "OXXO\nTOTAL 59.50",
        parsed = ParsedTicket(merchant, total, date, items, TicketParseSource.Regex),
        scanSource = ScanSource.Camera,
    )

    @Test
    fun `applyScan pre-fills concept, amount, date and items`() {
        val vm = viewModel()
        val date = LocalDate.now().minusDays(2)
        vm.applyScan(
            scan(
                date = date,
                items = listOf(
                    ParsedTicketItem("COCA 600ML", Money(1900)),
                    ParsedTicketItem("GANSITO", Money(2200)),
                ),
            ),
        )

        val st = vm.uiState.value
        assertEquals("OXXO", st.concept)
        assertEquals(59.50, st.amount!!, 0.0001)
        assertEquals(date, st.date)
        assertEquals(listOf("COCA 600ML", "GANSITO"), st.items.map { it.concept })
        assertEquals(listOf(1900L, 2200L), st.items.map { it.amount.cents })
        assertEquals("OXXO\nTOTAL 59.50", st.scanRawText)
        assertEquals(ScanSource.Camera, st.scanSource)
    }

    @Test
    fun `applyScan keeps current values where the parse found nothing`() {
        val vm = viewModel()
        vm.onConceptChange("Café")
        vm.applyScan(scan(merchant = null, total = null, date = null))

        val st = vm.uiState.value
        assertEquals("Café", st.concept)
        assertNull(st.amount)
        assertEquals(st.today, st.date)
    }

    @Test
    fun `applyScan clamps a future date to today`() {
        val vm = viewModel()
        vm.applyScan(scan(date = LocalDate.now().plusDays(5)))

        assertEquals(vm.uiState.value.today, vm.uiState.value.date)
    }

    @Test
    fun `onSave persists the scan raw text and source`() = runTest {
        val repository = FakeMovementRepository()
        val vm = viewModel(repository)
        vm.applyScan(scan())

        vm.onSave()
        advanceUntilIdle()

        val saved = repository.added.single()
        assertEquals("OXXO\nTOTAL 59.50", saved.scanRawText)
        assertEquals(ScanSource.Camera, saved.scanSource)
        assertEquals(5950L, saved.amount.cents)
        assertEquals("OXXO", saved.title)
    }

    // ---- Modo edición ----

    private fun existingMovement() = Movement(
        id = "m1",
        amount = Money(12550),
        categoryId = "cat-super",
        title = "Súper semanal",
        note = "Con vale de despensa",
        date = LocalDate.now().minusDays(3),
        items = listOf(MovementItem("i1", "Leche", Money(3000))),
        scanRawText = "SUPERAMA\nTOTAL 125.50",
        scanSource = ScanSource.Gallery,
    )

    @Test
    fun `edit mode pre-populates the form from the movement`() = runTest {
        val repository = FakeMovementRepository(initial = listOf(existingMovement()))
        val vm = viewModel(repository, editId = "m1")
        advanceUntilIdle()

        val st = vm.uiState.value
        assertTrue(st.isEditing)
        assertEquals("Súper semanal", st.concept)
        assertEquals(125.50, st.amount!!, 0.0001)
        assertEquals(LocalDate.now().minusDays(3), st.date)
        // La categoría no está en el catálogo: se preserva el id original.
        assertEquals("cat-super", st.category.id)
        assertEquals(listOf("Leche"), st.items.map { it.concept })
        assertEquals("Con vale de despensa", st.note)
        assertEquals("SUPERAMA\nTOTAL 125.50", st.scanRawText)
        assertEquals(ScanSource.Gallery, st.scanSource)
    }

    @Test
    fun `edit save calls update with the original id and preserves note and scan metadata`() = runTest {
        val repository = FakeMovementRepository(initial = listOf(existingMovement()))
        val vm = viewModel(repository, editId = "m1")
        advanceUntilIdle()

        vm.onConceptChange("Súper quincenal")
        vm.onSave()
        advanceUntilIdle()

        assertTrue(repository.added.isEmpty())
        val saved = repository.updated.single()
        assertEquals("m1", saved.id)
        assertEquals("Súper quincenal", saved.title)
        assertEquals("cat-super", saved.categoryId)
        assertEquals("Con vale de despensa", saved.note)
        assertEquals("SUPERAMA\nTOTAL 125.50", saved.scanRawText)
        assertEquals(ScanSource.Gallery, saved.scanSource)
    }

    @Test
    fun `edit with unknown id falls back to add mode`() = runTest {
        val repository = FakeMovementRepository()
        val vm = viewModel(repository, editId = "missing")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isEditing)

        vm.onConceptChange("Tacos")
        vm.onSave()
        advanceUntilIdle()

        assertTrue(repository.updated.isEmpty())
        val saved = repository.added.single()
        assertTrue(saved.id != "missing")
    }

    @Test
    fun `manual add still creates with a fresh id`() = runTest {
        val repository = FakeMovementRepository(initial = listOf(existingMovement()))
        val vm = viewModel(repository)
        vm.onConceptChange("Tacos")

        vm.onSave()
        advanceUntilIdle()

        assertTrue(repository.updated.isEmpty())
        assertTrue(repository.added.single().id != "m1")
    }

    @Test
    fun `manual flow saves without scan metadata`() = runTest {
        val repository = FakeMovementRepository()
        val vm = viewModel(repository)
        vm.onConceptChange("Tacos")

        vm.onSave()
        advanceUntilIdle()

        val saved = repository.added.single()
        assertNull(saved.scanRawText)
        assertNull(saved.scanSource)
        assertEquals("Tacos", saved.title)
        assertTrue(saved.items.isEmpty())
    }
}
