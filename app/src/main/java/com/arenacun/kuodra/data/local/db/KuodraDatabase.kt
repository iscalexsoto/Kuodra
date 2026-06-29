package com.arenacun.kuodra.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos local (fuente de verdad offline). Por ahora movimientos y categorías personales;
 * presupuestos e historial se añaden en Fase 3 subiendo la versión + migración.
 */
@Database(
    entities = [MovementEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class KuodraDatabase : RoomDatabase() {
    abstract fun movementDao(): MovementDao
    abstract fun categoryDao(): CategoryDao
}
