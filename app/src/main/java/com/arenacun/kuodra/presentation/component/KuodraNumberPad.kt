package com.arenacun.kuodra.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arenacun.kuodra.R
import com.arenacun.kuodra.domain.model.CalcKey
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors

/**
 * Teclado numérico ligero para capturar una **cantidad** (sin operadores ni teclado del
 * sistema): dígitos + punto + borrar + limpiar. Stateless: dibuja [state] y reenvía pulsaciones
 * por [onKey]. Reutiliza el motor puro [com.arenacun.kuodra.domain.model.Calc]; al usar solo el
 * subconjunto de teclas numéricas, la expresión es siempre un único número y `state.result` es
 * ese valor. Pensado para envolverse en un `Dialog` centrado, igual que [KuodraCalculator].
 */
@Composable
fun KuodraNumberPad(
    state: CalcState,
    title: String,
    confirmLabel: String,
    onKey: (CalcKey) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Kuodra.colors
    Column(
        modifier.fillMaxWidth().clip(Kuodra.shape.xxl).background(c.surface).padding(18.dp),
    ) {
        Text(title, style = Kuodra.type.overline, color = c.ink3,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

        // display
        Text(
            text = state.display,
            style = Kuodra.type.displayAmount,
            color = c.ink,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
                .clip(Kuodra.shape.lg).background(c.surface2)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )

        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PadButton(c, "7", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N7) }
                PadButton(c, "8", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N8) }
                PadButton(c, "9", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N9) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PadButton(c, "4", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N4) }
                PadButton(c, "5", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N5) }
                PadButton(c, "6", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N6) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PadButton(c, "1", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N1) }
                PadButton(c, "2", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N2) }
                PadButton(c, "3", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N3) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PadButton(c, ".", digit = true, Modifier.weight(1f)) { onKey(CalcKey.Dot) }
                PadButton(c, "0", digit = true, Modifier.weight(1f)) { onKey(CalcKey.N0) }
                PadIconButton(c, R.drawable.ic_backspace, Modifier.weight(1f)) { onKey(CalcKey.Back) }
            }
        }

        Box(
            Modifier.fillMaxWidth().padding(top = 14.dp).clip(Kuodra.shape.lg)
                .background(c.primary).clickable(onClick = onConfirm).padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text(confirmLabel, style = Kuodra.type.heading, color = c.primaryInk) }
    }
}

@Composable
private fun RowScope.PadButton(
    c: KuodraColors,
    label: String,
    digit: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    PadButtonBox(c, digit, modifier, onClick) {
        Text(label, style = Kuodra.type.heading, color = if (digit) c.ink else c.tintInk)
    }
}

/** Variante de [PadButton] que pinta un ícono `ic_*` (borrar) en vez de texto. */
@Composable
private fun RowScope.PadIconButton(
    c: KuodraColors,
    @DrawableRes icon: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    PadButtonBox(c, digit = false, modifier, onClick) {
        KIcon(icon, 22.dp, c.tintInk)
    }
}

@Composable
private fun PadButtonBox(
    c: KuodraColors,
    digit: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val bg: Color = if (digit) c.surface2 else c.tint
    Box(
        modifier.clip(Kuodra.shape.lg).background(bg).clickable(onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}
