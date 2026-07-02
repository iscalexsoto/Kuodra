package com.arenacun.kuodra.data.telemetry

import com.arenacun.kuodra.data.remote.dto.BreadcrumbDto
import org.junit.Assert.assertEquals
import org.junit.Test

class BreadcrumbBufferTest {

    private fun crumb(i: Int) = BreadcrumbDto(timestamp = i.toLong(), category = "c", message = "m$i")

    @Test
    fun `keeps only the last N breadcrumbs`() {
        val buffer = BreadcrumbBuffer(capacity = 3)
        (1..5).forEach { buffer.add(crumb(it)) }

        val snapshot = buffer.snapshot()

        assertEquals(3, snapshot.size)
        assertEquals(listOf("m3", "m4", "m5"), snapshot.map { it.message })
    }

    @Test
    fun `snapshot preserves insertion order`() {
        val buffer = BreadcrumbBuffer(capacity = 10)
        listOf(crumb(1), crumb(2), crumb(3)).forEach(buffer::add)

        assertEquals(listOf("m1", "m2", "m3"), buffer.snapshot().map { it.message })
    }
}
