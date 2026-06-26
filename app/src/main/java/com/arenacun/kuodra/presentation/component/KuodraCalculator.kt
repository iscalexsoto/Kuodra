package com.arenacun.kuodra.presentation.component

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
import com.arenacun.kuodra.domain.model.CalcKey
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors

/**
 * Numpad de la calculadora (`openCalc` del prototipo). Stateless: dibuja [state] y reenvía
 * pulsaciones por [onKey]. El motor vive en `domain` ([com.arenacun.kuodra.domain.model.Calc]).
 * Pensado para envolverse en un `Dialog` centrado desde la pantalla.
 */
@Composable
fun KuodraCalculator(
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
                CalcButton(c, "C", Variant.Clear, Modifier.weight(2f)) { onKey(CalcKey.Clear) }
                CalcButton(c, "←", Variant.Op, Modifier.weight(1f)) { onKey(CalcKey.Back) }
                CalcButton(c, "÷", Variant.Op, Modifier.weight(1f)) { onKey(CalcKey.Div) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CalcButton(c, "7", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.N7) }
                CalcButton(c, "8", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.N8) }
                CalcButton(c, "9", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.N9) }
                CalcButton(c, "×", Variant.Op, Modifier.weight(1f)) { onKey(CalcKey.Times) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CalcButton(c, "4", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.N4) }
                CalcButton(c, "5", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.N5) }
                CalcButton(c, "6", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.N6) }
                CalcButton(c, "−", Variant.Op, Modifier.weight(1f)) { onKey(CalcKey.Minus) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CalcButton(c, "1", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.N1) }
                CalcButton(c, "2", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.N2) }
                CalcButton(c, "3", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.N3) }
                CalcButton(c, "+", Variant.Op, Modifier.weight(1f)) { onKey(CalcKey.Plus) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CalcButton(c, "0", Variant.Digit, Modifier.weight(2f)) { onKey(CalcKey.N0) }
                CalcButton(c, ".", Variant.Digit, Modifier.weight(1f)) { onKey(CalcKey.Dot) }
                CalcButton(c, "=", Variant.Accent, Modifier.weight(1f)) { onKey(CalcKey.Equals) }
            }
        }

        Box(
            Modifier.fillMaxWidth().padding(top = 14.dp).clip(Kuodra.shape.lg)
                .background(c.primary).clickable(onClick = onConfirm).padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text(confirmLabel, style = Kuodra.type.heading, color = c.primaryInk) }
    }
}

private enum class Variant { Digit, Op, Clear, Accent }

@Composable
private fun RowScope.CalcButton(
    c: KuodraColors,
    label: String,
    variant: Variant,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val bg: Color = when (variant) {
        Variant.Digit -> c.surface2
        Variant.Op -> c.tint
        Variant.Clear -> c.negTint
        Variant.Accent -> c.primary
    }
    val ink: Color = when (variant) {
        Variant.Digit -> c.ink
        Variant.Op -> c.tintInk
        Variant.Clear -> c.neg
        Variant.Accent -> c.primaryInk
    }
    Box(
        modifier.clip(Kuodra.shape.lg).background(bg).clickable(onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Kuodra.type.heading, color = ink) }
}
