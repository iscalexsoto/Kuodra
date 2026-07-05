package com.arenacun.kuodra.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos local (fuente de verdad offline) del flujo Personal: movimientos, categorías,
 * presupuesto e historial de cortes, más la cola durable de telemetría. Sin migraciones en
 * pre-release: cualquier cambio de esquema sube [version] y recae en la migración destructiva
 * (el sync repuebla desde PocketBase; ver `DataModule`).
 */
@Database(
    entities = [
        MovementEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        PeriodSnapshotEntity::class,
        TelemetryEventEntity::class,
    ],
    // v7: columnas de escaneo en `movements` (scanRawText/scanSource). El bump fuerza la
    // migración destructiva ya configurada; el sync repuebla los datos desde PocketBase tras limpiar
    // (la telemetría pendiente que se pierda es aceptable: es diagnóstico best-effort).
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class KuodraDatabase : RoomDatabase() {
    abstract fun movementDao(): MovementDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun periodSnapshotDao(): PeriodSnapshotDao
    abstract fun telemetryDao(): TelemetryDao
}
