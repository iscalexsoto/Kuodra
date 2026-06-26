package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.arenacun.kuodra.presentation.theme.Kuodra

/** Avatar 40dp, pill, fondo tint con inicial en tintInk. */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier) {
    val colors = Kuodra.colors
    Box(
        modifier
            .size(40.dp)
            .clip(Kuodra.shape.pill)
            .background(colors.tint),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1).uppercase(),
            style = Kuodra.type.heading,
            color = colors.tintInk,
        )
    }
}

/**
 * Fila de lista: avatar + (título body / subtítulo caption) + monto.
 * El monto va en Space Grotesk, coloreado pos (entra) / neg (sale).
 * Colócala dentro de un KuodraCard; añade el divider entre filas.
 */
@Composable
fun KuodraListRow(
    name: String,
    sub: String,
    amount: String,
    positive: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = Kuodra.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(Kuodra.dims.gapRow),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name)
        Column(Modifier.weight(1f)) {
            Text(name, style = Kuodra.type.body, color = colors.ink)
            Text(sub, style = Kuodra.type.caption, color = colors.ink3)
        }
        Text(
            amount,
            style = Kuodra.type.heading,
            color = if (positive) colors.pos else colors.neg,
        )
    }
}
