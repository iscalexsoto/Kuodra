package com.arenacun.kuodra.domain.model

/**
 * Modo en que se reparte un gasto compartido entre los participantes. [None] = gasto no compartido
 * (Personal). Los otros modos son solo de **captura**: al guardar, la división siempre se resuelve a
 * centavos exactos ([SplitShare]) para que los balances no dependan del redondeo del dispositivo.
 */
enum class SplitMode { None, Equal, Amount, Percent }

/** Parte que puso un pagador de un gasto (referencia por id: [PersonRef.ME] o id de `persons`). */
data class PayerShare(val personId: String, val amount: Money)

/** Parte que le toca a un participante de la división, ya resuelta a centavos exactos. */
data class SplitShare(val personId: String, val share: Money)
