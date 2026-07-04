package com.arenacun.kuodra.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.arenacun.kuodra.R
import com.arenacun.kuodra.domain.model.ThemeMode
import com.arenacun.kuodra.presentation.theme.Kuodra

/** Una opción del switch de tema: modo + ícono + etiqueta. */
private data class ModeOption(val mode: ThemeMode, @DrawableRes val iconRes: Int, val label: String)

/**
 * Switch triple de tema (Sistema / Claro / Oscuro) con thumb deslizante animado, según el
 * handoff de diseño. Stateless: recibe el modo seleccionado y un callback. Lee solo tokens
 * `Kuodra.*`, así hereda claro/oscuro automáticamente.
 */
@Composable
fun KuodraThemeSwitch(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Kuodra.colors
    val options = listOf(
        ModeOption(ThemeMode.System, R.drawable.ic_tablet_smartphone, "Sistema"),
        ModeOption(ThemeMode.Light, R.drawable.ic_sun, "Claro"),
        ModeOption(ThemeMode.Dark, R.drawable.ic_moon, "Oscuro"),
    )
    val index = options.indexOfFirst { it.mode == selected }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(Kuodra.shape.lg)
            .background(c.screenBg)
            .padding(4.dp),
    ) {
        val slot = maxWidth / 3
        val thumbX by animateDpAsState(slot * index, tween(340), label = "thumb")

        // Overlay que adopta la altura resuelta por la Row (sin medir intrínsecos, que
        // BoxWithConstraints no soporta) y aloja el thumb deslizante detrás de las etiquetas.
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .offset(x = thumbX)
                    .width(slot)
                    .fillMaxHeight()
                    .shadow(2.dp, Kuodra.shape.md, clip = false)
                    .clip(Kuodra.shape.md)
                    .background(c.surface),
            )
        }

        Row(Modifier.fillMaxWidth()) {
            options.forEach { opt ->
                val active = opt.mode == selected
                Column(
                    Modifier
                        .weight(1f)
                        .clip(Kuodra.shape.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(opt.mode) }
                        .padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    KIcon(opt.iconRes, 20.dp, tint = if (active) c.primary else c.ink3)
                    Text(
                        opt.label,
                        style = Kuodra.type.caption,
                        color = if (active) c.ink else c.ink2,
                    )
                }
            }
        }
    }
}
