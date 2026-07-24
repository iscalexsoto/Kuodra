package com.arenacun.kuodra.data.sync

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.BudgetDao
import com.arenacun.kuodra.data.local.db.CategoryDao
import com.arenacun.kuodra.data.local.db.MovementDao
import com.arenacun.kuodra.data.local.db.PeriodSnapshotDao
import com.arenacun.kuodra.data.local.db.PersonDao
import com.arenacun.kuodra.data.local.db.SettlementDao
import com.arenacun.kuodra.data.local.db.SpaceDao
import com.arenacun.kuodra.data.mapper.toDto
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.remote.BudgetApi
import com.arenacun.kuodra.data.remote.CategoryApi
import com.arenacun.kuodra.data.remote.MovementApi
import com.arenacun.kuodra.data.remote.PeriodSnapshotApi
import com.arenacun.kuodra.data.remote.PersonApi
import com.arenacun.kuodra.data.remote.SettlementApi
import com.arenacun.kuodra.data.remote.SpaceApi

/**
 * Motor de sincronización (Kotlin puro, testeable). Por colección: **push** de las filas `dirty`
 * (crea o actualiza en PocketBase) y luego **pull** de los deltas (`updated > cursor`), haciendo
 * upsert en Room con *last-write-wins* (no pisa filas con cambios locales pendientes ni filas que
 * ya están en la versión remota — `remoteUpdated` — para que el pull de la misma corrida no
 * re-escriba lo recién subido) y respetando los tombstones (`deleted`). WorkManager solo lo
 * dispara; aquí vive toda la lógica.
 */
class SyncManager(
    private val movementApi: MovementApi,
    private val categoryApi: CategoryApi,
    private val budgetApi: BudgetApi,
    private val snapshotApi: PeriodSnapshotApi,
    private val spaceApi: SpaceApi,
    private val personApi: PersonApi,
    private val settlementApi: SettlementApi,
    private val movementDao: MovementDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val snapshotDao: PeriodSnapshotDao,
    private val spaceDao: SpaceDao,
    private val personDao: PersonDao,
    private val settlementDao: SettlementDao,
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
            runCatching { syncSpaces(owner, token) }.exceptionOrNull()?.let { add("spaces" to it) }
            runCatching { syncPersons(owner, token) }.exceptionOrNull()?.let { add("persons" to it) }
            runCatching { syncMovements(owner, token) }.exceptionOrNull()?.let { add("movements" to it) }
            runCatching { syncBudget(owner, token) }.exceptionOrNull()?.let { add("budgets" to it) }
            runCatching { syncSnapshots(owner, token) }.exceptionOrNull()?.let { add("period_snapshots" to it) }
            runCatching { syncSettlements(owner, token) }.exceptionOrNull()?.let { add("settlements" to it) }
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
            val local = categoryDao.find(dto.id)
            if (local?.dirty != true && local?.remoteUpdated != dto.updated) {
                categoryDao.upsert(dto.toEntity(owner))
            }
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
            // No pisar filas con cambios locales pendientes NI filas que ya están en la versión
            // remota (el push de esta misma corrida): si el servidor ignorara un campo del DTO
            // (p. ej. una columna aún no creada), el re-upsert borraría el dato local.
            val local = movementDao.find(dto.id)
            if (local?.dirty != true && local?.remoteUpdated != dto.updated) {
                movementDao.upsert(dto.toEntity(owner))
            }
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
            val local = budgetDao.find(owner)
            if (local?.dirty != true && local?.remoteUpdated != dto.updated) {
                budgetDao.upsert(dto.toEntity(owner))
            }
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
            val local = snapshotDao.find(dto.id)
            if (local?.dirty != true && local?.remoteUpdated != dto.updated) {
                snapshotDao.upsert(dto.toEntity(owner))
            }
            if (dto.updated > max) max = dto.updated
        }
        if (max != since) cursors.set(SNAPSHOTS, max)
    }

    private suspend fun syncSpaces(owner: String, token: String) {
        spaceDao.dirtyRows(owner).forEach { row ->
            val dto = row.toDto()
            val saved = push(row.remoteUpdated.isEmpty(),
                create = { spaceApi.create(dto, token) },
                update = { spaceApi.update(dto, token) })
            spaceDao.markSynced(row.id, saved.updated)
        }
        val since = cursors.get(SPACES)
        var max = since
        spaceApi.list(since, token).forEach { dto ->
            val local = spaceDao.find(dto.id)
            if (local?.dirty != true && local?.remoteUpdated != dto.updated) {
                spaceDao.upsert(dto.toEntity(owner))
            }
            if (dto.updated > max) max = dto.updated
        }
        if (max != since) cursors.set(SPACES, max)
    }

    private suspend fun syncPersons(owner: String, token: String) {
        personDao.dirtyRows(owner).forEach { row ->
            val dto = row.toDto()
            val saved = push(row.remoteUpdated.isEmpty(),
                create = { personApi.create(dto, token) },
                update = { personApi.update(dto, token) })
            personDao.markSynced(row.id, saved.updated)
        }
        val since = cursors.get(PERSONS)
        var max = since
        personApi.list(since, token).forEach { dto ->
            val local = personDao.find(dto.id)
            if (local?.dirty != true && local?.remoteUpdated != dto.updated) {
                personDao.upsert(dto.toEntity(owner))
            }
            if (dto.updated > max) max = dto.updated
        }
        if (max != since) cursors.set(PERSONS, max)
    }

    private suspend fun syncSettlements(owner: String, token: String) {
        settlementDao.dirtyRows(owner).forEach { row ->
            val dto = row.toDto()
            val saved = push(row.remoteUpdated.isEmpty(),
                create = { settlementApi.create(dto, token) },
                update = { settlementApi.update(dto, token) })
            settlementDao.markSynced(row.id, saved.updated)
        }
        val since = cursors.get(SETTLEMENTS)
        var max = since
        settlementApi.list(since, token).forEach { dto ->
            val local = settlementDao.find(dto.id)
            if (local?.dirty != true && local?.remoteUpdated != dto.updated) {
                settlementDao.upsert(dto.toEntity(owner))
            }
            if (dto.updated > max) max = dto.updated
        }
        if (max != since) cursors.set(SETTLEMENTS, max)
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
        const val SPACES = "spaces"
        const val PERSONS = "persons"
        const val SETTLEMENTS = "settlements"
    }
}
