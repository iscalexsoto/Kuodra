package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MapperTest {

    @Test
    fun `movement round-trips through entity preserving domain fields`() {
        val movement = Movement(
            id = "abc123def456ghi",
            amount = Money.ofMajor(119.80),
            categoryId = "super",
            title = "Súper",
            note = "Despensa",
            date = LocalDate.of(2026, 6, 20),
            payer = "Tú",
            splitNames = listOf("Tú", "Andrea"),
        )

        val entity = movement.toEntity(owner = "u1", updatedAt = 1_000L, dirty = true)

        assertEquals("u1", entity.owner)
        assertEquals(11980L, entity.amountCents)
        assertTrue(entity.dirty)
        assertFalse(entity.deleted)
        assertEquals(movement, entity.toDomain())
    }

    @Test
    fun `category round-trips through entity preserving tone`() {
        val category = Category("ocio", "Ocio", "Oc", AvatarTone.Neg)

        val entity = category.toEntity(owner = "u1", updatedAt = 1_000L, dirty = false)

        assertEquals("Neg", entity.tone)
        assertEquals(category, entity.toDomain())
    }
}
