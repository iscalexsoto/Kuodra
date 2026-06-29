package com.arenacun.kuodra.data.sync

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.CategoryDao
import com.arenacun.kuodra.data.local.db.MovementDao
import com.arenacun.kuodra.data.mapper.toDto
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.remote.CategoryApi
import com.arenacun.kuodra.data.remote.MovementApi

/**
 * Motor de sincronización (Kotlin puro, testeable). Por colección: **push** de las filas `dirty`
 * (crea o actualiza en PocketBase) y luego **pull** de los deltas (`updated > cursor`), haciendo
 * upsert en Room con *last-write-wins* (no pisa filas con cambios locales pendientes) y respetando
 * los tombstones (`deleted`). WorkManager solo lo dispara; aquí vive toda la lógica.
 */
class SyncManager(
    private val movementApi: MovementApi,
    private val categoryApi: CategoryApi,
    private val movementDao: MovementDao,
    private val categoryDao: CategoryDao,
    private val sessionStore: SessionStore,
    private val cursors: SyncCursorStore,
) {

    suspend fun sync(): Result<Unit> = runCatching {
        val token = sessionStore.token() ?: return@runCatching
        val owner = sessionStore.userId() ?: return@runCatching
        syncCategories(owner, token)
        syncMovements(owner, token)
    }

    private suspend fun syncCategories(owner: String, token: String) {
        categoryDao.dirtyRows(owner).forEach { row ->
            val dto = row.toDto()
            val saved = push(row.remoteUpdated.isEmpty(),
                create = { categoryApi.create(dto, token) },
                update = { categoryApi.update(dto, token) })
            categoryDao.markSynced(row.id, saved.updated)
        }
        val since = cursors.get(CATEGORIES)
        var max = since
        categoryApi.list(since, token).forEach { dto ->
            if (categoryDao.find(dto.id)?.dirty != true) categoryDao.upsert(dto.toEntity(owner))
            if (dto.updated > max) max = dto.updated
        }
        if (max != since) cursors.set(CATEGORIES, max)
    }

    private suspend fun syncMovements(owner: String, token: String) {
        movementDao.dirtyRows(owner).forEach { row ->
            val dto = row.toDto()
            val saved = push(row.remoteUpdated.isEmpty(),
                create = { movementApi.create(dto, token) },
                update = { movementApi.update(dto, token) })
            movementDao.markSynced(row.id, saved.updated)
        }
        val since = cursors.get(MOVEMENTS)
        var max = since
        movementApi.list(since, token).forEach { dto ->
            if (movementDao.find(dto.id)?.dirty != true) movementDao.upsert(dto.toEntity(owner))
            if (dto.updated > max) max = dto.updated
        }
        if (max != since) cursors.set(MOVEMENTS, max)
    }

    /** Crea o actualiza según corresponda, con fallback a la otra operación (id ya existe / no existe). */
    private suspend fun <T> push(isNew: Boolean, create: suspend () -> T, update: suspend () -> T): T =
        if (isNew) runCatching { create() }.getOrElse { update() }
        else runCatching { update() }.getOrElse { create() }

    private companion object {
        const val MOVEMENTS = "movements"
        const val CATEGORIES = "categories"
    }
}
