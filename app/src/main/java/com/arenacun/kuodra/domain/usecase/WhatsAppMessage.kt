package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Money

/**
 * Construye el texto de recordatorio de deuda para enviar por WhatsApp. Puro (el intent `wa.me`
 * vive en presentation). El [net] es el saldo de la persona respecto al grupo: **negativo** = te
 * debe (le cobras); **positivo** = le debes; cero = a mano.
 */
object WhatsAppMessage {

    fun build(personName: String, net: Money, spaceName: String): String {
        val amount = Calc.formatAmount(kotlin.math.abs(net.cents) / 100.0)
        return when {
            net.cents < 0L ->
                "Hola $personName, en \"$spaceName\" tu parte pendiente es $amount. ¿Me lo puedes pasar? ¡Gracias!"
            net.cents > 0L ->
                "Hola $personName, en \"$spaceName\" te debo $amount. Te lo paso en cuanto pueda."
            else ->
                "Hola $personName, en \"$spaceName\" ya quedamos a mano. ¡Gracias!"
        }
    }
}
