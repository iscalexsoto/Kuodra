package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.scan.ScanSource
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
    fun `scanned movement round-trips through entity and dto`() {
        val movement = Movement(
            id = "abc123def456ghi",
            amount = Money.ofMajor(59.50),
            categoryId = "super",
            title = "OXXO",
            date = LocalDate.of(2026, 7, 1),
            scanRawText = "OXXO\nTOTAL 59.50",
            scanSource = ScanSource.Camera,
        )

        val entity = movement.toEntity(owner = "u1", updatedAt = 1_000L, dirty = true)
        assertEquals("OXXO\nTOTAL 59.50", entity.scanRawText)
        assertEquals("Camera", entity.scanSource)
        assertEquals(movement, entity.toDomain())

        val dto = entity.toDto()
        assertEquals("OXXO\nTOTAL 59.50", dto.scanRawText)
        assertEquals("Camera", dto.scanSource)
        assertEquals(movement, dto.toEntity(owner = "u1").toDomain())
    }

    @Test
    fun `manual movement keeps scan fields null through dto round-trip`() {
        val movement = Movement(
            id = "abc123def456ghi",
            amount = Money.ofMajor(10.0),
            categoryId = "super",
            title = "Súper",
        )

        val entity = movement.toEntity(owner = "u1", updatedAt = 1_000L, dirty = false)
        val dto = entity.toDto()

        assertEquals("", dto.scanRawText)
        assertEquals("", dto.scanSource)
        val back = dto.toEntity(owner = "u1")
        assertEquals(null, back.scanRawText)
        assertEquals(null, back.scanSource)
        assertEquals(null, back.toDomain().scanSource)
    }

    @Test
    fun `category round-trips through entity preserving tone`() {
        val category = Category("ocio", "Ocio", "Oc", AvatarTone.Neg)

        val entity = category.toEntity(owner = "u1", updatedAt = 1_000L, dirty = false)

        assertEquals("Neg", entity.tone)
        assertEquals(category, entity.toDomain())
    }
}
