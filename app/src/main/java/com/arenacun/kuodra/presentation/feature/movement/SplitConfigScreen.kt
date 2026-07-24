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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.initialsOf
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.KuodraBottomSheet
import com.arenacun.kuodra.presentation.component.KuodraNumberPad
import com.arenacun.kuodra.presentation.component.PlusIcon
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors

/**
 * Pantalla dedicada de división de un gasto compartido: quién(es) pagaron (con monto) y cómo se
 * reparte (equitativo / montos / porcentajes). Comparte el [AddMovementViewModel] con AddMovement,
 * así que los cambios se reflejan al volver. "Listo" solo se habilita si pagadores y división cuadran.
 *
 * Con más de dos integrantes la lista se **colapsa**: cada sección muestra solo a los seleccionados
 * y un botón que abre una hoja para elegir a quién añadir (así el "equitativo" es realmente entre los
 * que participaron). Con dos integrantes o menos se muestran todos inline. Todos los montos (y el %)
 * se capturan tocando el campo → [KuodraNumberPad] (nunca teclado del sistema).
 */
@Composable
fun SplitConfigScreen(
    onBack: () -> Unit,
    viewModel: AddMovementViewModel,
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val payersError = viewModel.payersError(state)
    val splitError = viewModel.splitError(state)
    val collapsed = state.members.size > 2
    val singlePayer = state.payers.size == 1

    val equalShares = if (state.splitMode == SplitMode.Equal)
        com.arenacun.kuodra.domain.usecase.SplitCalc
            .resolveEqual(state.total, state.members.map { it.id }.filter { it in state.splitIds })
            .associate { it.personId to it.share } else emptyMap()

    Column(
        Modifier.fillMaxSize().background(c.screenBg).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 2.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = onBack)
            Text("Dividir gasto", style = Kuodra.type.heading, color = c.ink)
        }

        Text("Total del gasto: ${state.amountLabel}", style = Kuodra.type.caption, color = c.ink3,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))

        // ===== Pagadores =====
        SectionHeader(c, "¿QUIÉN PAGÓ?", payersError)
        if (collapsed) {
            state.payers.forEach { payer ->
                MemberRow(c, state.memberName(payer.personId)) {
                    PayerTrailing(c, payer, state.total, singlePayer) {
                        viewModel.onOpenSplitPad(SplitPadTarget(SplitPadKind.PayerAmount, payer.personId))
                    }
                }
            }
            AddButton(c, "Añadir o editar pagadores") { viewModel.onOpenSplitSheet(SplitSheet.AddPayer) }
        } else {
            state.members.forEach { member ->
                val payer = state.payers.firstOrNull { it.personId == member.id }
                SelectableRow(
                    c = c,
                    name = member.name,
                    selected = payer != null,
                    onToggle = { viewModel.onTogglePayer(member.id) },
                    trailing = {
                        if (payer != null) PayerTrailing(c, payer, state.total, singlePayer) {
                            viewModel.onOpenSplitPad(SplitPadTarget(SplitPadKind.PayerAmount, member.id))
                        }
                    },
                )
            }
        }

        Box(Modifier.padding(top = 18.dp))

        // ===== División =====
        SectionHeader(c, "¿ENTRE QUIÉNES?", splitError)
        Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(c, "Equitativo", state.splitMode == SplitMode.Equal) { viewModel.onSetSplitMode(SplitMode.Equal) }
            ModeChip(c, "Montos", state.splitMode == SplitMode.Amount) { viewModel.onSetSplitMode(SplitMode.Amount) }
            ModeChip(c, "%", state.splitMode == SplitMode.Percent) { viewModel.onSetSplitMode(SplitMode.Percent) }
        }

        if (collapsed) {
            state.members.filter { it.id in state.splitIds }.forEach { member ->
                MemberRow(c, member.name) {
                    SplitTrailing(c, state, member.id, equalShares[member.id] ?: Money.Zero, viewModel)
                }
            }
            AddButton(c, "Añadir o editar participantes") { viewModel.onOpenSplitSheet(SplitSheet.AddParticipant) }
        } else {
            state.members.forEach { member ->
                val included = member.id in state.splitIds
                SelectableRow(
                    c = c,
                    name = member.name,
                    selected = included,
                    onToggle = { viewModel.onToggleSplitMember(member.id) },
                    trailing = {
                        if (included) SplitTrailing(c, state, member.id, equalShares[member.id] ?: Money.Zero, viewModel)
                    },
                )
            }
        }

        val valid = payersError == null && splitError == null
        Box(
            Modifier.fillMaxWidth().padding(top = 22.dp).clip(Kuodra.shape.lg)
                .background(if (valid) c.primary else c.surface2)
                .clickable(enabled = valid, onClick = onBack).padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Listo", style = Kuodra.type.heading, color = if (valid) c.primaryInk else c.ink3)
        }
    }

    // ===== Overlays =====
    state.splitPadTarget?.let { target ->
        val name = state.memberName(target.personId)
        val title = when (target.kind) {
            SplitPadKind.PayerAmount -> "MONTO QUE PAGÓ $name"
            SplitPadKind.SplitAmount -> "MONTO DE $name"
            SplitPadKind.SplitPercent -> "PORCENTAJE DE $name"
        }
        Dialog(onDismissRequest = viewModel::onDismissSplitPad) {
            KuodraNumberPad(
                state = state.splitPad,
                title = title,
                confirmLabel = "Confirmar",
                onKey = viewModel::onSplitPadKey,
                onConfirm = viewModel::onConfirmSplitPad,
            )
        }
    }

    when (state.splitSheet) {
        SplitSheet.AddPayer -> KuodraBottomSheet(onDismiss = viewModel::onCloseSplitSheet) {
            PickerSheet(
                c = c,
                title = "¿Quién pagó?",
                subtitle = "Marca a quién puso dinero. Con varios pagadores indicarás cuánto puso cada uno.",
                members = state.members,
                isSelected = { id -> state.payers.any { it.personId == id } },
                onToggle = viewModel::onTogglePayer,
                onDone = viewModel::onCloseSplitSheet,
            )
        }
        SplitSheet.AddParticipant -> KuodraBottomSheet(onDismiss = viewModel::onCloseSplitSheet) {
            PickerSheet(
                c = c,
                title = "¿Entre quiénes?",
                subtitle = "Marca a quiénes se les reparte el gasto.",
                members = state.members,
                isSelected = { id -> id in state.splitIds },
                onToggle = viewModel::onToggleSplitMember,
                onDone = viewModel::onCloseSplitSheet,
            )
        }
        null -> {}
    }
}

@Composable
private fun SectionHeader(c: KuodraColors, label: String, error: String?) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Kuodra.type.overline, color = c.ink3)
        if (error != null) Text(error, style = Kuodra.type.caption, color = c.neg)
    }
}

/** Fila con toggle por tap (avatar/nombre): usada inline cuando hay ≤2 integrantes y en las hojas. */
@Composable
private fun SelectableRow(
    c: KuodraColors,
    name: String,
    selected: Boolean,
    onToggle: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(Kuodra.shape.lg)
            .background(if (selected) c.tint else c.surface)
            .border(1.dp, if (selected) c.primary else c.line, Kuodra.shape.lg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clickable(onClick = onToggle)) {
            ToneAvatar(initialsOf(name), toneForName(name), size = 34.dp)
        }
        Text(name, style = Kuodra.type.body, color = c.ink,
            modifier = Modifier.weight(1f).clickable(onClick = onToggle))
        trailing()
    }
}

/** Fila de un miembro ya seleccionado (sin toggle): la gestión se hace en la hoja de selección. */
@Composable
private fun MemberRow(c: KuodraColors, name: String, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(Kuodra.shape.lg)
            .background(c.tint).border(1.dp, c.primary, Kuodra.shape.lg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToneAvatar(initialsOf(name), toneForName(name), size = 34.dp)
        Text(name, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Botón "+ Añadir…" que abre la hoja de selección de pagadores / participantes. */
@Composable
private fun AddButton(c: KuodraColors, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(Kuodra.shape.lg)
            .border(1.dp, c.line, Kuodra.shape.lg)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusIcon(14.dp, c.primary)
        Text(label, style = Kuodra.type.body, color = c.primary)
    }
}

/** Trailing de un pagador: total no editable si es el único; si no, chip tappable → number pad. */
@Composable
private fun PayerTrailing(
    c: KuodraColors,
    payer: PayerShare,
    total: Money,
    singlePayer: Boolean,
    onEdit: () -> Unit,
) {
    if (singlePayer) {
        Text(Calc.formatAmount(total.major), style = Kuodra.type.heading, color = c.ink)
    } else {
        ValueChip(c, Calc.formatAmount(payer.amount.major), onEdit)
    }
}

/** Trailing de un participante según el modo: equitativo (texto) / montos / % (chip tappable). */
@Composable
private fun SplitTrailing(
    c: KuodraColors,
    state: AddMovementUiState,
    id: String,
    equalShare: Money,
    viewModel: AddMovementViewModel,
) {
    when (state.splitMode) {
        SplitMode.Equal -> Text(
            Calc.formatAmount(equalShare.major), style = Kuodra.type.heading, color = c.ink,
        )
        SplitMode.Amount -> ValueChip(c, Calc.formatAmount(Money(state.amountDraft[id] ?: 0L).major)) {
            viewModel.onOpenSplitPad(SplitPadTarget(SplitPadKind.SplitAmount, id))
        }
        SplitMode.Percent -> ValueChip(c, "${state.percentDraft[id] ?: 0}%") {
            viewModel.onOpenSplitPad(SplitPadTarget(SplitPadKind.SplitPercent, id))
        }
        SplitMode.None -> {}
    }
}

/** Chip tappable que muestra un valor formateado y abre el number pad para editarlo. */
@Composable
private fun ValueChip(c: KuodraColors, text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(Kuodra.shape.md).background(c.surface2)
            .border(1.dp, c.line, Kuodra.shape.md)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Kuodra.type.heading, color = c.ink) }
}

@Composable
private fun ModeChip(c: KuodraColors, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(Kuodra.shape.pill)
            .background(if (selected) c.primary else c.surface2)
            .border(1.dp, if (selected) c.primary else c.line, Kuodra.shape.pill)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = Kuodra.type.caption, color = if (selected) c.primaryInk else c.ink)
    }
}

/** Contenido de la hoja de selección: checklist de todos los miembros con toggle por tap. */
@Composable
private fun PickerSheet(
    c: KuodraColors,
    title: String,
    subtitle: String,
    members: List<SpacePerson>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text(title, style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 4.dp))
        Text(subtitle, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(bottom = 12.dp))
        members.forEach { member ->
            SelectableRow(
                c = c,
                name = member.name,
                selected = isSelected(member.id),
                onToggle = { onToggle(member.id) },
                trailing = { CheckDot(c, isSelected(member.id)) },
            )
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 10.dp).clip(Kuodra.shape.lg).background(c.primary)
                .clickable(onClick = onDone).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Listo", style = Kuodra.type.heading, color = c.primaryInk) }
    }
}

/** Indicador de selección (círculo) para las filas de la hoja de selección. */
@Composable
private fun CheckDot(c: KuodraColors, checked: Boolean) {
    Box(
        Modifier.padding(end = 2.dp).clip(Kuodra.shape.pill)
            .background(if (checked) c.primary else c.surface2)
            .border(1.dp, if (checked) c.primary else c.line, Kuodra.shape.pill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (checked) "✓" else "＋", style = Kuodra.type.caption,
            color = if (checked) c.primaryInk else c.ink3)
    }
}
