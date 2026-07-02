package com.arenacun.kuodra.data.telemetry

import com.arenacun.kuodra.data.remote.dto.BreadcrumbDto

/**
 * Buffer circular en memoria de las últimas [capacity] migas. Thread-safe: se escribe desde
 * cualquier hilo (logs, red, UI) y se lee al construir un evento. Al llenarse, descarta las viejas.
 */
class BreadcrumbBuffer(private val capacity: Int = 50) {

    private val items = ArrayDeque<BreadcrumbDto>(capacity)

    @Synchronized
    fun add(crumb: BreadcrumbDto) {
        items.addLast(crumb)
        while (items.size > capacity) items.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<BreadcrumbDto> = items.toList()
}
