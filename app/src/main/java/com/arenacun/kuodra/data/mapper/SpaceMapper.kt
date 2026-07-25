package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.local.db.SpaceEntity
import com.arenacun.kuodra.data.remote.dto.SpaceDto
import com.arenacun.kuodra.data.remote.dto.SplitRuleDto
import com.arenacun.kuodra.data.remote.dto.SplitRuleShareDto
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitRule
import com.arenacun.kuodra.domain.model.SplitRuleShare
import com.arenacun.kuodra.domain.model.UseCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/** Nombre del enum ⇄ modo de regla, tolerante (vacío/desconocido/`Amount`/`None` ⇒ Equal). */
private fun ruleModeOf(name: String): SplitMode =
    if (name == SplitMode.Percent.name) SplitMode.Percent else SplitMode.Equal

private fun SplitRule.toDto(): SplitRuleDto = SplitRuleDto(
    enabled = enabled,
    mode = mode.name,
    shares = shares.map { SplitRuleShareDto(it.personId, it.percent) },
    payer = payerId,
    autoPersonalCopy = autoPersonalCopy,
)

private fun SplitRuleDto.toDomain(): SplitRule = SplitRule(
    enabled = enabled,
    mode = ruleModeOf(mode),
    shares = shares.map { SplitRuleShare(it.personId, it.percent) },
    payerId = payer,
    autoPersonalCopy = autoPersonalCopy,
)

/**
 * Regla (dominio) ⇄ JSON de la columna `splitRuleJson`. Se serializa el **DTO**, no el modelo de
 * dominio, para que la columna y el campo json de PocketBase tengan la misma forma (así `toDto`
 * puede re-decodificar la columna directamente).
 */
internal fun SplitRule.toRuleJson(): String = json.encodeToString(toDto())

internal fun String.toSplitRule(): SplitRule =
    if (isBlank()) SplitRule.Default else json.decodeFromString<SplitRuleDto>(this).toDomain()

/** Entity → dominio (los espacios de Room son siempre de Gastos). */
fun SpaceEntity.toDomain(): Space = Space(
    id = id,
    useCase = UseCase.Gastos,
    name = name,
    archived = archived,
    reminderEnabled = reminderEnabled,
    splitRule = splitRuleJson.toSplitRule(),
)

/** Entity → DTO (push). PocketBase ignora los campos de sistema (`updated`). */
fun SpaceEntity.toDto(): SpaceDto = SpaceDto(
    id = id,
    owner = owner,
    name = name,
    archived = archived,
    reminderEnabled = reminderEnabled,
    splitRule = json.decodeFromString(splitRuleJson.ifBlank { "{}" }),
    deleted = deleted,
    updated = remoteUpdated,
)

/** DTO → Entity (pull): ya sincronizado (`dirty = false`), con el `updated` remoto. */
fun SpaceDto.toEntity(owner: String): SpaceEntity = SpaceEntity(
    id = id,
    owner = owner.ifEmpty { this.owner },
    name = name,
    archived = archived,
    reminderEnabled = reminderEnabled,
    splitRuleJson = json.encodeToString(splitRule),
    updatedAt = System.currentTimeMillis(),
    deleted = deleted,
    dirty = false,
    remoteUpdated = updated,
)
