package com.arenacun.kuodra.presentation.feature.movement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.KuodraNumberPad
import com.arenacun.kuodra.presentation.component.PlusIcon
import com.arenacun.kuodra.presentation.theme.Kuodra

/**
 * Pantalla dedicada del **detalle** (partidas) de un movimiento. Comparte el [AddMovementViewModel]
 * con AddMovement (los cambios se reflejan al volver). Solo la **lista de partidas** scrollea; el
 * "Ajuste" y el botón "Listo" quedan **fijos** abajo. Al volver/"Listo" se descartan las partidas
 * vacías ([AddMovementViewModel.onCloseDetail]).
 */
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: AddMovementViewModel,
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val done = { viewModel.onCloseDetail(); onBack() }

    Column(Modifier.fillMaxSize().background(c.screenBg).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.padding(start = 2.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = done)
            Text("Detalle", style = Kuodra.type.heading, color = c.ink)
        }

        // ===== Lista scrollable (solo esta parte crece/scrollea) =====
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(
                "Desglosa el gasto en partidas. Lo no detallado queda como Ajuste.",
                style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(bottom = 10.dp, start = 2.dp),
            )

            state.items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Kuodra.shape.lg)
                        .background(c.surface).border(1.dp, c.line, Kuodra.shape.lg)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = item.concept,
                        onValueChange = { viewModel.onItemConcept(item.id, it) },
                        singleLine = true,
                        textStyle = Kuodra.type.body.copy(color = c.ink),
                        cursorBrush = SolidColor(c.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (item.concept.isEmpty()) Text("Concepto", style = Kuodra.type.body, color = c.ink3)
                            inner()
                        },
                    )
                    Box(
                        Modifier.clip(Kuodra.shape.md).background(c.surface2)
                            .clickable { viewModel.onOpenItemPad(item.id) }.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(Calc.formatAmount(item.amount.major), style = Kuodra.type.heading, color = c.ink) }
                    Box(
                        Modifier.size(28.dp).clip(Kuodra.shape.pill).background(c.negTint)
                            .clickable { viewModel.onRemoveItem(item.id) },
                        contentAlignment = Alignment.Center,
                    ) { Text("×", style = Kuodra.type.heading, color = c.neg) }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Kuodra.shape.lg)
                    .border(1.dp, c.line, Kuodra.shape.lg)
                    .clickable(onClick = viewModel::onAddItem).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlusIcon(14.dp, c.primary)
                Text("Añadir partida", style = Kuodra.type.body, color = c.primary)
            }
        }

        // ===== Footer fijo: Ajuste + Listo =====
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp).clip(Kuodra.shape.lg)
                .background(c.tint).padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Ajuste", style = Kuodra.type.body, color = c.tintInk)
                Text("Total no detallado", style = Kuodra.type.caption, color = c.tintInk)
            }
            Text(Calc.formatAmount(state.adjustment.major), style = Kuodra.type.heading, color = c.tintInk)
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp).clip(Kuodra.shape.lg).background(c.primary)
                .clickable(onClick = done).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Listo", style = Kuodra.type.heading, color = c.primaryInk) }
    }

    // Number pad de la cantidad de una partida (overlay, se movió aquí desde AddMovement).
    if (state.showNumberPad) {
        Dialog(onDismissRequest = viewModel::onDismissPad) {
            KuodraNumberPad(
                state = state.pad,
                title = "CANTIDAD DE LA PARTIDA",
                confirmLabel = "Confirmar",
                onKey = viewModel::onPadKey,
                onConfirm = viewModel::onConfirmItemAmount,
            )
        }
    }
}
