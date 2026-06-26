package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.arenacun.kuodra.presentation.theme.Kuodra

/**
 * Hero card del dashboard: bg primary, 24r, texto primaryInk, monto displayAmount.
 * Opcional: chip de ciclo translúcido arriba. Los círculos decorativos del prototipo
 * (rgba blanco .07–.08) puedes recrearlos con un Canvas de fondo si los quieres.
 */
@Composable
fun KuodraHeroCard(
    amount: String,
    caption: String,
    cycleChip: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = Kuodra.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(Kuodra.shape.xxl) // 24
            .background(colors.primary)
            .padding(22.dp),
    ) {
        if (cycleChip != null) {
            Box(
                Modifier
                    .clip(Kuodra.shape.pill)
                    .background(colors.primaryInk.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(cycleChip, style = Kuodra.type.caption, color = colors.primaryInk)
            }
        }
        Text(
            amount,
            style = Kuodra.type.displayAmount,
            color = colors.primaryInk,
            modifier = Modifier.padding(top = if (cycleChip != null) 12.dp else 0.dp),
        )
        Text(
            caption,
            style = Kuodra.type.caption,
            color = colors.primaryInk.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
