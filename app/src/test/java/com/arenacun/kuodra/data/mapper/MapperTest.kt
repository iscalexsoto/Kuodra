package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.ReturnStatus
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.SettlementBalanceLine
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitShare
import com.arenacun.kuodra.domain.model.Transfer
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
    fun `return fields round-trip through entity and dto`() {
        val movement = Movement(
            id = "abc123def456ghi",
            amount = Money.ofMajor(90.0),
            categoryId = "super",
            title = "Súper",
            items = listOf(
                MovementItem("i1", "A", Money(60_00), returnable = true),
                MovementItem("i2", "B", Money(30_00), returnable = false),
            ),
            returnStatus = ReturnStatus.Returned,
            returnPercent = 70,
        )

        val entity = movement.toEntity(owner = "u1", updatedAt = 1_000L, dirty = false)
        assertEquals("Returned", entity.returnStatus)
        assertEquals(70, entity.returnPercent)
        assertEquals(movement, entity.toDomain())

        val dto = entity.toDto()
        assertEquals("Returned", dto.returnStatus)
        assertEquals(70, dto.returnPercent)
        assertFalse(dto.items[1].returnable)
        assertEquals(movement, dto.toEntity(owner = "u1").toDomain())
    }

    @Test
    fun `none movement carries zero percent through the dto`() {
        val movement = Movement(
            id = "abc123def456ghi",
            amount = Money.ofMajor(10.0),
            categoryId = "super",
            title = "Súper",
        )

        val dto = movement.toEntity(owner = "u1", updatedAt = 1_000L, dirty = false).toDto()
        assertEquals("None", dto.returnStatus)
        assertEquals(0, dto.returnPercent)
        assertEquals(movement, dto.toEntity(owner = "u1").toDomain())
    }

    @Test
    fun `server empty return status and zero percent decode to none and null`() {
        // PocketBase devuelve text vacío / number 0 en campos no poblados (registros legacy).
        val dto = com.arenacun.kuodra.data.remote.dto.MovementDto(
            id = "abc123def456ghi",
            amount = 1000,
            category = "super",
            title = "Súper",
            returnStatus = "",
            returnPercent = 0,
        )

        val back = dto.toEntity(owner = "u1")
        assertEquals("None", back.returnStatus)
        assertEquals(null, back.returnPercent)
        assertEquals(ReturnStatus.None, back.toDomain().returnStatus)
    }

    @Test
    fun `legacy item json without returnable decodes to true`() {
        val entity = Movement(
            id = "abc123def456ghi",
            amount = Money.ofMajor(50.0),
            categoryId = "super",
            title = "Súper",
        ).toEntity(owner = "u1", updatedAt = 1_000L, dirty = false)
            .copy(itemsJson = """[{"id":"i1","concept":"A","amount":5000}]""")

        val items = entity.toDomain().items
        assertEquals(1, items.size)
        assertTrue(items[0].returnable)
    }

    @Test
    fun `category round-trips through entity preserving tone`() {
        val category = Category("ocio", "Ocio", "Oc", AvatarTone.Neg)

        val entity = category.toEntity(owner = "u1", updatedAt = 1_000L, dirty = false)

        assertEquals("Neg", entity.tone)
        assertEquals(category, entity.toDomain())
    }

    @Test
    fun `shared expense round-trips payers splits and settlement through entity and dto`() {
        val movement = Movement(
            id = "abc123def456ghi",
            amount = Money.ofMajor(900.0),
            categoryId = "super",
            title = "Súper",
            spaceId = "s1",
            payers = listOf(PayerShare(PersonRef.ME, Money(90000))),
            splitMode = SplitMode.Equal,
            splits = listOf(SplitShare(PersonRef.ME, Money(30000)), SplitShare("a", Money(60000))),
            settlementId = "set1",
        )

        val entity = movement.toEntity(owner = "u1", updatedAt = 1_000L, dirty = true)
        assertEquals("s1", entity.space)
        assertEquals("Equal", entity.splitMode)
        assertEquals("set1", entity.settlementId)
        assertEquals(movement, entity.toDomain())

        val dto = entity.toDto()
        assertEquals("s1", dto.space)
        assertEquals(2, dto.splits.size)
        assertEquals(movement, dto.toEntity(owner = "u1").toDomain())
    }

    @Test
    fun `person round-trips through entity and dto`() {
        val person = SpacePerson(id = "p1", name = "Andrea", phone = "+521555")

        val entity = person.toEntity(owner = "u1", space = "s1", updatedAt = 1_000L, dirty = true)
        assertEquals("s1", entity.space)
        assertEquals(person, entity.toDomain())

        val dto = entity.toDto()
        assertEquals("+521555", dto.phone)
        assertEquals(person, dto.toEntity(owner = "u1").toDomain())
    }

    @Test
    fun `settlement round-trips lines and transfers through entity and dto`() {
        val settlement = Settlement(
            id = "set1",
            spaceId = "s1",
            title = "Liquidación de junio",
            date = LocalDate.of(2026, 6, 30),
            total = Money(90000),
            lines = listOf(
                SettlementBalanceLine(PersonRef.ME, "Tú", Money(30000)),
                SettlementBalanceLine("a", "Andrea", Money(-30000)),
            ),
            transfers = listOf(Transfer("a", PersonRef.ME, Money(30000))),
            createdAt = 5_000L,
        )

        val entity = settlement.toEntity(owner = "u1", updatedAt = 1_000L, dirty = true)
        assertEquals("s1", entity.space)
        assertEquals(90000L, entity.totalCents)
        assertEquals(settlement, entity.toDomain())

        val dto = entity.toDto()
        assertEquals(2, dto.lines.size)
        assertEquals(1, dto.transfers.size)
        assertEquals(settlement, dto.toEntity(owner = "u1").toDomain())
    }
}
