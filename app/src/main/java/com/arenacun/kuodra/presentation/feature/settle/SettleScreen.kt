package com.arenacun.kuodra.presentation.feature.settle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.KuodraNumberPad
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettleScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: SettleViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.done.collect { onDone() } }
    LaunchedEffect(Unit) {
        viewModel.whatsapp.collect { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        }
    }

    Column(
        Modifier.fillMaxSize().background(c.screenBg).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 2.dp, top = 6.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = onBack)
            Text(state.title, style = Kuodra.type.heading, color = c.ink)
        }

        // hero
        Column(
            Modifier.fillMaxWidth().clip(Kuodra.shape.xxl).background(c.primary).padding(22.dp),
        ) {
            Text(state.heroLabel, style = Kuodra.type.caption, color = c.primaryInk.copy(alpha = 0.85f))
            Text(state.heroAmount, style = Kuodra.type.displayAmount, color = c.primaryInk,
                modifier = Modifier.padding(top = 6.dp))
            if (state.owedAmount != null) {
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(c, "Te deben", state.owedAmount!!, Modifier.weight(1f))
                    Stat(c, "Debes", state.oweAmount ?: "$0", Modifier.weight(1f))
                }
            }
        }

        Text(if (state.useCase == UseCase.Gastos) "QUIÉN DEBE A QUIÉN" else "MOVIMIENTOS POR PERSONA",
            style = Kuodra.type.overline, color = c.ink3,
            modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp))

        Column(
            Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface).border(1.dp, c.line, Kuodra.shape.xl),
        ) {
            state.people.forEachIndexed { i, row ->
                val p = row.person
                // Primera línea: persona + saldo. Segunda línea: acciones (para que el nombre no se aplaste).
                Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToneAvatar(p.initials, p.tone)
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = Kuodra.type.body, color = c.ink)
                            Text(p.sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
                        }
                        Text(p.amount, style = Kuodra.type.heading,
                            color = when (p.positive) { true -> c.pos; false -> c.neg; null -> c.ink })
                    }
                    if (row.hasPhone || row.netCents != 0L) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (row.netCents != 0L) {
                                ActionButton("Liquidar", c.tint, c.tintInk, Modifier.weight(1f)) {
                                    viewModel.onOpenPay(row.personId)
                                }
                            }
                            if (row.hasPhone) {
                                ActionButton("WhatsApp", c.posTint, c.pos, Modifier.weight(1f)) {
                                    viewModel.onWhatsApp(row.personId)
                                }
                            }
                        }
                    }
                }
                if (i < state.people.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            }
        }

        Spacer(Modifier.height(22.dp))
        val canRegister = state.canRegister
        Box(
            Modifier.fillMaxWidth().clip(Kuodra.shape.lg)
                .background(if (canRegister) c.primary else c.surface2)
                .then(if (canRegister) Modifier.clickable { viewModel.onRegister() } else Modifier)
                .padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(state.confirmLabel, style = Kuodra.type.heading,
                color = if (canRegister) c.primaryInk else c.ink3)
        }
        if (!canRegister) {
            Text("No hay saldos pendientes por liquidar.", style = Kuodra.type.caption, color = c.ink3,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        }
    }

    // Confirmación de la liquidación total.
    if (state.showRegisterConfirm) {
        Dialog(onDismissRequest = viewModel::onDismissRegister) {
            Column(Modifier.clip(Kuodra.shape.xl).background(c.surface).padding(22.dp)) {
                Text("¿Registrar liquidación?", style = Kuodra.type.heading, color = c.ink)
                Text("Se cerrará el periodo: los saldos quedan saldados y el corte se guarda en el Historial. No podrás editarlo después.",
                    style = Kuodra.type.caption, color = c.ink2, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.surface2)
                            .border(1.dp, c.line, Kuodra.shape.lg)
                            .clickable(onClick = viewModel::onDismissRegister).padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Cancelar", style = Kuodra.type.heading, color = c.ink) }
                    Box(
                        Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.primary)
                            .clickable(onClick = viewModel::onConfirmRegister).padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Registrar", style = Kuodra.type.heading, color = c.primaryInk) }
                }
            }
        }
    }

    // Number pad del pago por persona.
    if (state.payPadPersonId != null) {
        val name = state.people.firstOrNull { it.personId == state.payPadPersonId }?.person?.name ?: ""
        Dialog(onDismissRequest = viewModel::onDismissPay) {
            KuodraNumberPad(
                state = state.payPad,
                title = "PAGO DE ${name.uppercase()}",
                confirmLabel = "Registrar pago",
                onKey = viewModel::onPayKey,
                onConfirm = viewModel::onConfirmPay,
            )
        }
    }
}

/** Botón de acción por persona (WhatsApp / Liquidar), en la segunda línea de la fila. */
@Composable
private fun ActionButton(
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.clip(Kuodra.shape.md).background(bg).clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Kuodra.type.caption, color = fg) }
}

@Composable
private fun Stat(c: KuodraColors, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(Kuodra.shape.md).background(c.primaryInk.copy(alpha = 0.14f))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Text(label, style = Kuodra.type.caption, color = c.primaryInk.copy(alpha = 0.8f))
        Text(value, style = Kuodra.type.heading, color = c.primaryInk, modifier = Modifier.padding(top = 2.dp))
    }
}
