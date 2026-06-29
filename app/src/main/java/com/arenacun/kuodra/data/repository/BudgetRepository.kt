package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.BudgetDao
import com.arenacun.kuodra.data.mapper.toConfig
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.BudgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Presupuesto Personal respaldado en Room (fuente de verdad) y sincronizado. Si el usuario aún no
 * configuró nada, emite [BudgetConfig.Default] (apagado).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepository(
    private val dao: BudgetDao,
    private val sessionStore: SessionStore,
    private val syncTrigger: SyncTrigger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val budget: StateFlow<BudgetConfig> =
        sessionStore.sessionFlow.flatMapLatest { session ->
            if (session == null) flowOf(BudgetConfig.Default)
            else dao.observe(session.userId).map { it?.toConfig() ?: BudgetConfig.Default }
        }.stateIn(scope, SharingStarted.Eagerly, BudgetConfig.Default)

    suspend fun update(config: BudgetConfig) {
        val owner = sessionStore.userId() ?: return
        dao.upsert(config.toEntity(owner, System.currentTimeMillis(), dirty = true))
        syncTrigger.requestSync()
    }
}
