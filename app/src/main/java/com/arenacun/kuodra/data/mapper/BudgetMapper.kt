package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.local.db.BudgetEntity
import com.arenacun.kuodra.data.remote.dto.BudgetDto
import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Money

private fun frequencyOf(name: String): BudgetFrequency =
    runCatching { BudgetFrequency.valueOf(name) }.getOrDefault(BudgetFrequency.Biweekly)

fun BudgetEntity.toConfig(): BudgetConfig = BudgetConfig(
    enabled = enabled,
    frequency = frequencyOf(frequency),
    amount = Calc.formatAmount(amountCents / 100.0),
    weekday = weekday,
    firstDay = firstDay,
    secondDay = secondDay,
    monthlyDay = monthlyDay,
    customInterval = customInterval,
)

fun BudgetConfig.toEntity(
    owner: String,
    updatedAt: Long,
    dirty: Boolean,
    deleted: Boolean = false,
    remoteUpdated: String = "",
): BudgetEntity = BudgetEntity(
    owner = owner,
    enabled = enabled,
    frequency = frequency.name,
    amountCents = Money.ofMajor(Calc.parseAmount(amount) ?: 0.0).cents,
    weekday = weekday,
    firstDay = firstDay,
    secondDay = secondDay,
    monthlyDay = monthlyDay,
    customInterval = customInterval,
    updatedAt = updatedAt,
    deleted = deleted,
    dirty = dirty,
    remoteUpdated = remoteUpdated,
)

/** Entity → DTO (push). El id del registro en PocketBase es el `owner`. */
fun BudgetEntity.toDto(): BudgetDto = BudgetDto(
    id = owner,
    owner = owner,
    enabled = enabled,
    frequency = frequency,
    amount = amountCents,
    weekday = weekday,
    firstDay = firstDay,
    secondDay = secondDay,
    monthlyDay = monthlyDay,
    customInterval = customInterval,
    deleted = deleted,
    updated = remoteUpdated,
)

fun BudgetDto.toEntity(owner: String): BudgetEntity = BudgetEntity(
    owner = owner.ifEmpty { this.owner },
    enabled = enabled,
    frequency = frequency,
    amountCents = amount,
    weekday = weekday,
    firstDay = firstDay,
    secondDay = secondDay,
    monthlyDay = monthlyDay,
    customInterval = customInterval,
    updatedAt = System.currentTimeMillis(),
    deleted = deleted,
    dirty = false,
    remoteUpdated = updated,
)
