package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.PeriodSnapshot
import kotlinx.coroutines.flow.StateFlow

/**
 * Periodos cerrados (historial Personal). Respaldado por Room (fuente de verdad) y sincronizado.
 * Se expone como [StateFlow] para lectura síncrona/reactiva.
 */
interface SnapshotRepository {
    val snapshots: StateFlow<List<PeriodSnapshot>>
    suspend fun add(snapshot: PeriodSnapshot)
}
