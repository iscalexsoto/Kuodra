package com.arenacun.kuodra.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos local (fuente de verdad offline). Por ahora movimientos y categorías personales;
 * presupuestos e historial se añaden en Fase 3 subiendo la versión + migración.
 */
@Database(
    entities = [
        MovementEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        PeriodSnapshotEntity::class,
    ],
    // v4: sin cambios de esquema; el bump fuerza una migración destructiva única para que el
    // callback limpie los cursores y el sync repueble desde PocketBase (recupera instalaciones
    // que quedaron con Room vacío + cursores viejos tras el bump a v3).
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class KuodraDatabase : RoomDatabase() {
    abstract fun movementDao(): MovementDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun periodSnapshotDao(): PeriodSnapshotDao
}
