package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.KuodraSeedSource
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepositoryImpl(
    private val seed: KuodraSeedSource,
) : SettingsRepository {

    private val flows = mutableMapOf<UseCase, MutableStateFlow<SpaceSettings>>()

    private fun flowFor(useCase: UseCase): MutableStateFlow<SpaceSettings> =
        flows.getOrPut(useCase) { MutableStateFlow(seed.settings(useCase)) }

    override fun settings(useCase: UseCase): StateFlow<SpaceSettings> = flowFor(useCase)

    override fun update(useCase: UseCase, settings: SpaceSettings) {
        flowFor(useCase).value = settings
    }

    override fun history(useCase: UseCase): List<SettlementRecord> = seed.history(useCase)

    override fun historyEntry(useCase: UseCase, id: String): SettlementRecord? =
        seed.history(useCase).find { it.id == id }
}
