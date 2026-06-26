package com.arenacun.kuodra.domain.model

data class Person(
    val name: String,
    val sub: String,
    val amount: String,
    val positive: Boolean?,   // null = monto neutro (color ink)
    val initials: String,
    val tone: AvatarTone,
)
