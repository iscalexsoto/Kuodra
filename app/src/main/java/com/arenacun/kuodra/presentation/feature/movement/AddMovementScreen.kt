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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.MovementCategory
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.CategoryTag
import com.arenacun.kuodra.presentation.component.Chevron
import com.arenacun.kuodra.presentation.component.KuodraBottomSheet
import com.arenacun.kuodra.presentation.component.KuodraCalculator
import com.arenacun.kuodra.presentation.component.KuodraCalendar
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddMovementScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddMovementViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val space by viewModel.space.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uc = space.useCase
    val t = space.terminology

    LaunchedEffect(Unit) { viewModel.saved.collect { onSaved() } }

    val dateSel = when (state.date) {
        state.today -> 0
        state.today.minusDays(1) -> 1
        else -> 2
    }

    Column(
        Modifier.fillMaxSize().background(c.screenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 2.dp, top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = onBack)
            Text(t.addTitle, style = Kuodra.type.heading, color = c.ink)
        }

        // FECHA
        Text("FECHA", style = Kuodra.type.overline, color = c.ink3,
            modifier = Modifier.padding(start = 2.dp, bottom = 9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DateChip(c, "Hoy", DateLabels.dayMonth(state.today), dateSel == 0, Modifier.weight(1f)) {
                viewModel.onPickToday()
            }
            DateChip(c, "Ayer", DateLabels.dayMonth(state.today.minusDays(1)), dateSel == 1, Modifier.weight(1f)) {
                viewModel.onPickYesterday()
            }
            DateChip(c, "Otra", if (dateSel == 2) DateLabels.dayMonth(state.date) else "Elegir",
                dateSel == 2, Modifier.weight(1f)) {
                viewModel.onOpenCalendar()
            }
        }

        // MONTO (toca para calcular)
        Column(
            Modifier.fillMaxWidth().padding(top = 14.dp)
                .clip(Kuodra.shape.xl).background(c.tint)
                .border(1.5.dp, c.primary, Kuodra.shape.xl)
                .clickable { viewModel.onOpenCalculator() }
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("TOCA PARA CALCULAR →", style = Kuodra.type.overline, color = c.tintInk)
            Text(state.amountLabel, style = Kuodra.type.displayAmount,
                color = if (state.hasAmount) c.ink else c.ink3,
                modifier = Modifier.padding(top = 2.dp))
        }

        // concepto + categoría
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp).clip(Kuodra.shape.xl)
                .background(c.surface).border(1.dp, c.line, Kuodra.shape.xl)
                .padding(horizontal = 16.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                Text("Concepto", style = Kuodra.type.caption, color = c.ink3)
                BasicTextField(
                    value = state.concept,
                    onValueChange = viewModel::onConceptChange,
                    singleLine = true,
                    textStyle = Kuodra.type.body.copy(color = c.ink),
                    cursorBrush = SolidColor(c.primary),
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                    decorationBox = { inner ->
                        if (state.concept.isEmpty()) Text("¿En qué se gastó?", style = Kuodra.type.body, color = c.ink3)
                        inner()
                    },
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.onOpenSheet(AddSheet.Category) }
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryTag(state.category.tag, state.category.tone, size = 34.dp)
                Column(Modifier.weight(1f)) {
                    Text("Categoría", style = Kuodra.type.caption, color = c.ink3)
                    Text(state.category.name, style = Kuodra.type.body, color = c.ink,
                        modifier = Modifier.padding(top = 1.dp))
                }
                Chevron(7.dp, c.ink3, degrees = 90f)
            }
        }

        // payer (gastos/caja)
        if (uc != UseCase.Personal) {
            FieldRow(
                c,
                leading = { ToneAvatar(if (state.payer == "Tú") "T" else state.payer.take(1), toneForName(state.payer), size = 34.dp) },
                label = t.paidLabel,
                value = state.payer,
                onClick = { viewModel.onOpenSheet(AddSheet.Payer) },
            )
        }
        // dividir entre (gastos)
        if (uc == UseCase.Gastos) {
            FieldRow(
                c,
                leading = {
                    Box(Modifier.size(34.dp).clip(Kuodra.shape.md)
                        .background(Kuodra.colors.posTint), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(11.dp).clip(Kuodra.shape.pill).background(c.pos))
                    }
                },
                label = "Dividir entre",
                value = state.splitNames.joinToString(", ").ifBlank { "Nadie" },
                onClick = { viewModel.onOpenSheet(AddSheet.Split) },
            )
        }
        // contra el fondo (caja)
        if (uc == UseCase.Caja) {
            FieldRow(
                c,
                leading = {
                    Box(Modifier.size(34.dp).clip(Kuodra.shape.md).background(c.tint),
                        contentAlignment = Alignment.Center) {
                        Box(Modifier.size(width = 14.dp, height = 11.dp).clip(Kuodra.shape.sm)
                            .border(2.dp, c.tintInk, Kuodra.shape.sm))
                    }
                },
                label = "Contra el fondo",
                value = "${space.displayName} · $900 disponibles",
                onClick = {},
            )
        }

        // save
        Box(
            Modifier.fillMaxWidth().padding(top = 22.dp).clip(Kuodra.shape.lg).background(c.primary)
                .clickable { viewModel.onSave() }.padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Guardar ${t.saveNoun}", style = Kuodra.type.heading, color = c.primaryInk) }
    }

    // ===== Overlays =====
    if (state.showCalculator) {
        Dialog(onDismissRequest = viewModel::onDismissCalculator) {
            KuodraCalculator(
                state = state.calc,
                title = "CALCULAR MONTO",
                confirmLabel = "Confirmar monto",
                onKey = viewModel::onCalcKey,
                onConfirm = viewModel::onConfirmAmount,
            )
        }
    }
    if (state.showCalendar) {
        Dialog(onDismissRequest = viewModel::onDismissCalendar) {
            KuodraCalendar(
                selected = state.date,
                today = state.today,
                onPick = viewModel::onPickDate,
            )
        }
    }
    when (state.sheet) {
        AddSheet.Category -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            CategorySheet(c, state.categories, state.category) { viewModel.onPickCategory(it) }
        }
        AddSheet.Payer -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            PickerSheet(c, t.paidLabel.ifBlank { "¿Quién pagó?" }, state.members, state.payer) { viewModel.onPickPayer(it) }
        }
        AddSheet.Split -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            SplitSheet(c, state.members, state.splitNames, viewModel::onToggleSplit, viewModel::onCloseSheet)
        }
        null -> {}
    }
}

@Composable
private fun DateChip(
    c: KuodraColors,
    top: String,
    sub: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.clip(Kuodra.shape.lg)
            .background(if (selected) c.tint else c.surface)
            .border(1.5.dp, if (selected) c.primary else c.line, Kuodra.shape.lg)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(top, style = Kuodra.type.heading, color = if (selected) c.primary else c.ink)
        Text(sub, style = Kuodra.type.overline, color = if (selected) c.primary else c.ink3,
            modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FieldRow(
    c: KuodraColors,
    leading: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).clip(Kuodra.shape.lg)
            .background(c.surface).border(1.dp, c.line, Kuodra.shape.lg)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(label, style = Kuodra.type.caption, color = c.ink3)
            Text(value, style = Kuodra.type.body, color = c.ink, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
        }
        Chevron(7.dp, c.ink3, degrees = 90f)
    }
}

@Composable
private fun CategorySheet(
    c: KuodraColors,
    categories: List<MovementCategory>,
    selected: MovementCategory,
    onPick: (MovementCategory) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text("Categoría", style = Kuodra.type.heading, color = c.ink,
            modifier = Modifier.padding(bottom = 8.dp))
        categories.forEach { cat ->
            val isSel = cat.name == selected.name
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Kuodra.shape.lg)
                    .background(if (isSel) c.tint else c.surface)
                    .border(1.dp, if (isSel) c.primary else c.line, Kuodra.shape.lg)
                    .clickable { onPick(cat) }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryTag(cat.tag, cat.tone, size = 34.dp)
                Text(cat.name, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                if (isSel) Chevron(8.dp, c.primary, degrees = 0f)
            }
        }
    }
}

@Composable
private fun PickerSheet(
    c: KuodraColors,
    title: String,
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text(title, style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 8.dp))
        options.forEach { name ->
            val isSel = name == selected
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Kuodra.shape.lg)
                    .background(if (isSel) c.tint else c.surface)
                    .border(1.dp, if (isSel) c.primary else c.line, Kuodra.shape.lg)
                    .clickable { onPick(name) }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToneAvatar(if (name == "Tú") "T" else name.take(1), toneForName(name), size = 34.dp)
                Text(name, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                if (isSel) Chevron(8.dp, c.primary, degrees = 0f)
            }
        }
    }
}

@Composable
private fun SplitSheet(
    c: KuodraColors,
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text("Dividir entre", style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 8.dp))
        options.forEach { name ->
            val isSel = name in selected
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Kuodra.shape.lg)
                    .background(c.surface).border(1.dp, c.line, Kuodra.shape.lg)
                    .clickable { onToggle(name) }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToneAvatar(if (name == "Tú") "T" else name.take(1), toneForName(name), size = 34.dp)
                Text(name, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                CheckMark(c, isSel)
            }
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 14.dp).clip(Kuodra.shape.lg).background(c.primary)
                .clickable(onClick = onDone).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Listo", style = Kuodra.type.heading, color = c.primaryInk) }
    }
}

@Composable
private fun CheckMark(c: KuodraColors, checked: Boolean) {
    Box(
        Modifier.size(22.dp).clip(Kuodra.shape.sm)
            .background(if (checked) c.primary else c.surface)
            .border(1.5.dp, if (checked) c.primary else c.line, Kuodra.shape.sm),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Text("✓", style = Kuodra.type.caption, color = c.primaryInk, textAlign = TextAlign.Center)
    }
}
