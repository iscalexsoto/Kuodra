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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.initialsOf
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors

/**
 * Pantalla dedicada de división de un gasto compartido: quién(es) pagaron (con monto) y cómo se
 * reparte (equitativo / montos / porcentajes). Comparte el [AddMovementViewModel] con AddMovement,
 * así que los cambios se reflejan al volver. "Listo" solo se habilita si pagadores y división cuadran.
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
        state.members.forEach { member ->
            val payer = state.payers.firstOrNull { it.personId == member.id }
            SelectableRow(
                c = c,
                name = member.name,
                selected = payer != null,
                onToggle = { viewModel.onTogglePayer(member.id) },
                trailing = {
                    if (payer != null) {
                        MoneyInput(c, payer.amount) { cents -> viewModel.onSetPayerAmount(member.id, cents) }
                    }
                },
            )
        }

        Box(Modifier.padding(top = 18.dp))

        // ===== División =====
        SectionHeader(c, "¿ENTRE QUIÉNES?", splitError)
        Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(c, "Equitativo", state.splitMode == SplitMode.Equal) { viewModel.onSetSplitMode(SplitMode.Equal) }
            ModeChip(c, "Montos", state.splitMode == SplitMode.Amount) { viewModel.onSetSplitMode(SplitMode.Amount) }
            ModeChip(c, "%", state.splitMode == SplitMode.Percent) { viewModel.onSetSplitMode(SplitMode.Percent) }
        }

        val equalShares = if (state.splitMode == SplitMode.Equal)
            com.arenacun.kuodra.domain.usecase.SplitCalc
                .resolveEqual(state.total, state.members.map { it.id }.filter { it in state.splitIds })
                .associate { it.personId to it.share } else emptyMap()

        state.members.forEach { member ->
            val included = member.id in state.splitIds
            SelectableRow(
                c = c,
                name = member.name,
                selected = included,
                onToggle = { viewModel.onToggleSplitMember(member.id) },
                trailing = {
                    if (included) when (state.splitMode) {
                        SplitMode.Equal -> Text(
                            Calc.formatAmount((equalShares[member.id] ?: Money.Zero).major),
                            style = Kuodra.type.heading, color = c.ink,
                        )
                        SplitMode.Amount -> MoneyInput(c, Money(state.amountDraft[member.id] ?: 0L)) { cents ->
                            viewModel.onSetSplitAmount(member.id, cents)
                        }
                        SplitMode.Percent -> PercentInput(c, state.percentDraft[member.id] ?: 0) { pct ->
                            viewModel.onSetSplitPercent(member.id, pct)
                        }
                        SplitMode.None -> {}
                    }
                },
            )
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

/** Campo de monto en pesos → centavos. */
@Composable
private fun MoneyInput(c: KuodraColors, value: Money, onChange: (Long) -> Unit) {
    val text = if (value.cents == 0L) "" else value.major.let {
        if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
    }
    Box(
        Modifier.width(96.dp).clip(Kuodra.shape.md).background(c.surface2)
            .border(1.dp, c.line, Kuodra.shape.md).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { raw ->
                val pesos = Calc.parseAmount(raw.filter { it.isDigit() || it == '.' }) ?: 0.0
                onChange(Money.ofMajor(pesos).cents)
            },
            singleLine = true,
            textStyle = Kuodra.type.heading.copy(color = c.ink, textAlign = TextAlign.End),
            cursorBrush = SolidColor(c.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$", style = Kuodra.type.heading, color = c.ink3)
                    Box(Modifier.weight(1f)) {
                        if (text.isEmpty()) Text("0", style = Kuodra.type.heading, color = c.ink3,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                        inner()
                    }
                }
            },
        )
    }
}

/** Campo de porcentaje entero. */
@Composable
private fun PercentInput(c: KuodraColors, value: Int, onChange: (Int) -> Unit) {
    Box(
        Modifier.width(72.dp).clip(Kuodra.shape.md).background(c.surface2)
            .border(1.dp, c.line, Kuodra.shape.md).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        BasicTextField(
            value = if (value == 0) "" else value.toString(),
            onValueChange = { raw -> onChange(raw.filter { it.isDigit() }.toIntOrNull() ?: 0) },
            singleLine = true,
            textStyle = Kuodra.type.heading.copy(color = c.ink, textAlign = TextAlign.End),
            cursorBrush = SolidColor(c.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (value == 0) Text("0", style = Kuodra.type.heading, color = c.ink3,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                        inner()
                    }
                    Text("%", style = Kuodra.type.heading, color = c.ink3)
                }
            },
        )
    }
}
