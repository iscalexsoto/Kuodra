package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import kotlinx.coroutines.flow.StateFlow

/**
 * Ajustes del espacio por caso de uso, observables. El contrato es mínimo: la UI lee el
 * flujo y persiste el [SpaceSettings] completo tras cada edición (la lógica de edición vive
 * en el ViewModel). Cuando exista persistencia real, el impl combina local + remoto.
 */
interface SettingsRepository {
    fun settings(useCase: UseCase): StateFlow<SpaceSettings>
    fun update(useCase: UseCase, settings: SpaceSettings)

    /** Historial de periodos cerrados (cortes/liquidaciones) del caso de uso. */
    fun history(useCase: UseCase): List<SettlementRecord>

    /** Un registro del historial por id. */
    fun historyEntry(useCase: UseCase, id: String): SettlementRecord?
}
