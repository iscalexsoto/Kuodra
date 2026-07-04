package com.arenacun.kuodra.presentation.feature.scan

import androidx.lifecycle.ViewModel
import com.arenacun.kuodra.domain.scan.TicketScan

/**
 * Holder del resultado de un escaneo, compartido entre `ScanTicket` y `AddMovement` con scope al
 * grafo `AddGraph` (se limpia solo al hacer pop del grafo, igual que el AuthViewModel del auth).
 * En el flujo manual nunca se setea y [consume] devuelve null: el formulario arranca vacío.
 */
class ScanDraftViewModel : ViewModel() {

    private var draft: TicketScan? = null

    fun set(scan: TicketScan) {
        draft = scan
    }

    /** Entrega el draft **una sola vez** (evita re-aplicarlo en recomposiciones o regresos). */
    fun consume(): TicketScan? = draft.also { draft = null }
}
