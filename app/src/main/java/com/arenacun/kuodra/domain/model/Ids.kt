package com.arenacun.kuodra.domain.model

/**
 * Genera un id **compatible con PocketBase** (15 caracteres `[a-z0-9]`) en el dispositivo. Así un
 * registro creado offline ya tiene su id final y se usa igual en local y remoto: al sincronizar no
 * hace falta remapear. Kotlin puro.
 */
private const val ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"

fun newId(length: Int = 15): String = buildString(length) {
    repeat(length) { append(ID_ALPHABET.random()) }
}
