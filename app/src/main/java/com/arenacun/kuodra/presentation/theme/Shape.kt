package com.arenacun.kuodra.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object KuodraShapes {
    val sm = RoundedCornerShape(8.dp)    // chips de monto, swatches
    val md = RoundedCornerShape(12.dp)   // contenedores de ícono
    val lg = RoundedCornerShape(16.dp)   // botones, inputs
    val xl = RoundedCornerShape(18.dp)   // cards de contenido
    val xxl = RoundedCornerShape(24.dp)  // hero card
    val pill = RoundedCornerShape(50)    // pills, FAB, avatares (50%)
}

object Dimens {
    val gapIcon = 8.dp    // gap entre íconos / chips
    val gapList = 12.dp   // gap entre cards de lista
    val gapRow = 14.dp    // gap interno de filas
    val cardPad = 16.dp   // padding de card
    val screenPad = 22.dp // padding lateral de pantalla
}
