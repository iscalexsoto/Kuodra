package com.arenacun.kuodra.data.sync

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.BudgetDao
import com.arenacun.kuodra.data.local.db.CategoryDao
import com.arenacun.kuodra.data.local.db.MovementDao
import com.arenacun.kuodra.data.local.db.PeriodSnapshotDao
import com.arenacun.kuodra.data.mapper.toDto
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.remote.BudgetApi
import com.arenacun.kuodra.data.remote.CategoryApi
import com.arenacun.kuodra.data.remote.MovementApi
import com.arenacun.kuodra.data.remote.PeriodSnapshotApi

/**
 * Motor de sincronización (Kotlin puro, testeable). Por colección: **push** de las filas `dirty`
 * (crea o actualiza en PocketBase) y luego **pull** de los deltas (`updated > cursor`), haciendo
 * upsert en Room con *last-write-wins* (no pisa filas con cambios locales pendientes) y respetando
 * los tombstones (`deleted`). WorkManager solo lo dispara; aquí vive toda la lógica.
 */
class SyncManager(
    private val movementApi: MovementApi,
    private val categoryApi: CategoryApi,
    private val budgetApi: BudgetApi,
    private val snapshotApi: PeriodSnapshotApi,
    private val movementDao: MovementDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val snapshotDao: PeriodSnapshotDao,
    private val sessionStore: SessionStore,
    private val cursors: SyncCursorStore,
) {

    suspend fun sync(): Result<Unit> = runCatching {
        val token = sessionStore.token()
        val owner = sessionStore.userId()
        if (token == null || owner == null) {
            android.util.Log.w("KuodraSync", "sync omitido: token=${token != null} owner=${owner != null}")
            return@runCatching
        }
        android.util.Log.d("KuodraSync", "sync start owner=$owner")
        // Cada colección se sincroniza de forma aislada: si una falla (p. ej. un registro que no
        // deserializa), las demás igual avanzan. Se relanza el primer error para que el worker
        // reintente y la causa se registre.
        val errors = buildList {
            runCatching { syncCategories(owner, token) }.exceptionOrNull()?.let { add("categories" to it) }
            runCatching { syncMovements(owner, token) }.exceptionOrNull()?.let { add("movements" to it) }
            runCatching { syncBudget(owner, token) }.exceptionOrNull()?.let { add("budgets" to it) }
            runCatching { syncSnapshots(owner, token) }.exceptionOrNull()?.let { add("period_snapshots" to it) }
        }
        errors.forEach { (collection, error) ->
            android.util.Log.w("KuodraSync", "Falló la colección '$collection'", error)
        }
        errors.firstOrNull()?.let { throw it.second }
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
        val dirty = movementDao.dirtyRows(owner)
        dirty.forEach { row ->
            val dto = row.toDto()
            val saved = push(row.remoteUpdated.isEmpty(),
                create = { movementApi.create(dto, token) },
                update = { movementApi.update(dto, token) })
            movementDao.markSynced(row.id, saved.updated)
        }
        val since = cursors.get(MOVEMENTS)
        var max = since
        val remote = movementApi.list(since, token)
        android.util.Log.d("KuodraSync", "movements push=${dirty.size} pull=${remote.size} since='$since'")
        remote.forEach { dto ->
            if (movementDao.find(dto.id)?.dirty != true) movementDao.upsert(dto.toEntity(owner))
            if (dto.updated > max) max = dto.updated
        }
        if (max != since) cursors.set(MOVEMENTS, max)
    }

    private suspend fun syncBudget(owner: String, token: String) {
        val dirty = budgetDao.dirtyRows(owner)
        dirty.forEach { row ->
            val dto = row.toDto()
            val saved = push(row.remoteUpdated.isEmpty(),
                create = { budgetApi.create(dto, token) },
                update = { budgetApi.update(dto, token) })
            budgetDao.markSynced(owner, saved.updated)
        }
        val since = cursors.get(BUDGETS)
        var max = since
        val remote = budgetApi.list(since, token)
        android.util.Log.d("KuodraSync", "budgets push=${dirty.size} pull=${remote.size}")
        remote.forEach { dto ->
            budgetDao.upsert(dto.toEntity(owner))
            if (dto.updated > max) max = dto.updated
        }
        if (max != since) cursors.set(BUDGETS, max)
    }

    private suspend fun syncSnapshots(owner: String, token: String) {
        val dirty = snapshotDao.dirtyRows(owner)
        dirty.forEach { row ->
            val dto = row.toDto()
            val saved = push(row.remoteUpdated.isEmpty(),
                create = { snapshotApi.create(dto, token) },
                update = { snapshotApi.update(dto, token) })
            snapshotDao.markSynced(row.id, saved.updated)
        }
        val since = cursors.get(SNAPSHOTS)
        var max = since
        val remote = snapshotApi.list(since, token)
        android.util.Log.d("KuodraSync", "period_snapshots push=${dirty.size} pull=${remote.size}")
        remote.forEach { dto ->
            if (snapshotDao.find(dto.id)?.dirty != true) snapshotDao.upsert(dto.toEntity(owner))
            if (dto.updated > max) max = dto.updated
        }
        if (max != since) cursors.set(SNAPSHOTS, max)
    }

    /** Crea o actualiza según corresponda, con fallback a la otra operación (id ya existe / no existe). */
    private suspend fun <T> push(isNew: Boolean, create: suspend () -> T, update: suspend () -> T): T =
        if (isNew) runCatching { create() }.getOrElse { update() }
        else runCatching { update() }.getOrElse { create() }

    private companion object {
        const val MOVEMENTS = "movements"
        const val CATEGORIES = "categories"
        const val BUDGETS = "budgets"
        const val SNAPSHOTS = "period_snapshots"
    }
}
