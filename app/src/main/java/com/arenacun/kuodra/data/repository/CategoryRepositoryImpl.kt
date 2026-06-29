package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.CategoryDao
import com.arenacun.kuodra.data.mapper.toDomain
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.repository.CategoryRepository
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
 * Catálogo de categorías respaldado en Room (fuente de verdad). Filtra por `owner` siguiendo la
 * sesión y **siembra** los defaults en el primer arranque de cada usuario (marcados `dirty` para
 * que suban al sincronizar en Fase 2).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryRepositoryImpl(
    private val dao: CategoryDao,
    private val sessionStore: SessionStore,
    private val syncTrigger: SyncTrigger,
) : CategoryRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Catálogo del usuario, siempre con [Category.Uncategorized] al frente. */
    override val categories: StateFlow<List<Category>> =
        sessionStore.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(listOf(Category.Uncategorized))
            } else {
                dao.observe(session.userId).map { rows ->
                    listOf(Category.Uncategorized) + rows.map { it.toDomain() }
                }
            }
        }.stateIn(scope, SharingStarted.Eagerly, listOf(Category.Uncategorized))

    override suspend fun add(category: Category) = upsert(category)

    override suspend fun update(category: Category) = upsert(category)

    override suspend fun delete(id: String) {
        if (id == Category.Uncategorized.id) return // la estática no se borra
        dao.softDelete(id, System.currentTimeMillis())
        syncTrigger.requestSync()
    }

    private suspend fun upsert(category: Category) {
        if (category.isStatic) return // la estática no se edita
        val owner = sessionStore.userId() ?: return
        dao.upsert(category.toEntity(owner, System.currentTimeMillis(), dirty = true))
        syncTrigger.requestSync()
    }
}
