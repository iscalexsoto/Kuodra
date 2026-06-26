package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import com.arenacun.kuodra.presentation.theme.Kuodra

/** Botón circular de "atrás" usado en los encabezados del flujo. */
@Composable
fun BackCircle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Kuodra.colors
    Box(
        modifier
            .size(36.dp)
            .clip(Kuodra.shape.pill)
            .background(c.surface2)
            .border(1.dp, c.line, Kuodra.shape.pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Chevron(12.dp, c.ink2, degrees = 180f) }
}
