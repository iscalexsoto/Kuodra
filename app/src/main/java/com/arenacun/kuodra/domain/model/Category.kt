package com.arenacun.kuodra.domain.model

data class Category(
    val name: String,
    val sub: String,
    val amount: String,
    val pct: Float,           // 0f..1f
    val tag: String,
    val tone: AvatarTone,
)
