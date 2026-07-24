package com.arenacun.kuodra.domain.model

/**
 * Estado de devolución de un movimiento (solo flujo Personal). [None] = no participa; [Pending]
 * = "Por devolver" (su reembolso se calcula en vivo con el % global vigente); [Returned] =
 * "Devuelto" (el % quedó congelado en [Movement.returnPercent]). Los textos de display viven en
 * la capa de presentación (`MovementUi`), no en el enum.
 */
enum class ReturnStatus { None, Pending, Returned }
