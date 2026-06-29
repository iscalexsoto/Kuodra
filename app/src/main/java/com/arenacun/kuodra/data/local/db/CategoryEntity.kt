package com.arenacun.kuodra.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila persistida de una categoría del catálogo. Mismas columnas de sincronización que
 * [MovementEntity]. [tone] guarda el nombre del enum `AvatarTone`.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val name: String,
    val tag: String,
    val tone: String,
    val archived: Boolean,
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    /** `updated` del servidor de la última sincronización (vacío = nunca subido). */
    val remoteUpdated: String = "",
)
