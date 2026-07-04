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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.CategoryEditSheet
import com.arenacun.kuodra.presentation.component.CategoryTag
import com.arenacun.kuodra.presentation.component.Chevron
import com.arenacun.kuodra.presentation.component.PlusIcon
import com.arenacun.kuodra.presentation.component.KuodraBottomSheet
import com.arenacun.kuodra.presentation.component.KuodraCalculator
import com.arenacun.kuodra.presentation.component.KuodraCalendar
import com.arenacun.kuodra.presentation.component.KuodraNumberPad
import com.arenacun.kuodra.presentation.component.KuodraSearchField
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.feature.scan.ScanDraftViewModel
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddMovementScreen(
    draftViewModel: ScanDraftViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddMovementViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val space by viewModel.space.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uc = space.useCase
    val t = space.terminology

    // Si venimos de un escaneo, el draft pre-puebla el formulario (una sola vez; en el flujo
    // manual `consume()` devuelve null y no pasa nada). En edición el draft se ignora: un
    // escaneo nunca pisa un movimiento existente.
    LaunchedEffect(Unit) {
        if (!viewModel.isEditMode) draftViewModel.consume()?.let(viewModel::applyScan)
    }

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
            Text(
                if (state.isEditing) "Editar ${t.saveNoun}" else t.addTitle,
                style = Kuodra.type.heading, color = c.ink,
            )
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

        // detalle (partidas)
        if (state.items.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(Kuodra.shape.lg)
                    .border(1.dp, c.line, Kuodra.shape.lg)
                    .clickable { viewModel.onAddItem(); viewModel.onOpenSheet(AddSheet.Detail) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(34.dp).clip(Kuodra.shape.md).background(c.surface2),
                    contentAlignment = Alignment.Center) {
                    PlusIcon(15.dp, c.primary)
                }
                Text("Añadir detalle", style = Kuodra.type.body, color = c.primary)
            }
        } else {
            FieldRow(
                c,
                leading = {
                    Box(Modifier.size(34.dp).clip(Kuodra.shape.md).background(c.tint),
                        contentAlignment = Alignment.Center) {
                        Text("${state.items.size}", style = Kuodra.type.heading, color = c.tintInk)
                    }
                },
                label = "Detalle",
                value = "${state.items.size} ${if (state.items.size == 1) "partida" else "partidas"} · Ajuste ${Calc.formatAmount(state.adjustment.major)}",
                onClick = { viewModel.onOpenSheet(AddSheet.Detail) },
            )
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
        ) {
            Text(
                if (state.isEditing) "Guardar cambios" else "Guardar ${t.saveNoun}",
                style = Kuodra.type.heading, color = c.primaryInk,
            )
        }
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
            val draft = state.editingCategory
            if (draft != null) {
                CategoryEditSheet(
                    c = c,
                    draft = draft,
                    onName = viewModel::onCategoryDraftName,
                    onTone = viewModel::onCategoryDraftTone,
                    onConfirm = viewModel::onConfirmCreateCategory,
                )
            } else {
                CategorySheet(
                    c = c,
                    categories = state.categories,
                    selected = state.category,
                    onPick = viewModel::onPickCategory,
                    onCreate = viewModel::onStartCreateCategory,
                )
            }
        }
        AddSheet.Payer -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            PickerSheet(c, t.paidLabel.ifBlank { "¿Quién pagó?" }, state.members, state.payer) { viewModel.onPickPayer(it) }
        }
        AddSheet.Split -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            SplitSheet(c, state.members, state.splitNames, viewModel::onToggleSplit, viewModel::onCloseSheet)
        }
        AddSheet.Detail -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            DetailSheet(
                c = c,
                items = state.items,
                adjustment = Calc.formatAmount(state.adjustment.major),
                onConcept = viewModel::onItemConcept,
                onAmount = viewModel::onOpenItemPad,
                onRemove = viewModel::onRemoveItem,
                onAdd = viewModel::onAddItem,
                onDone = viewModel::onCloseSheet,
            )
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
    categories: List<Category>,
    selected: Category,
    onPick: (Category) -> Unit,
    onCreate: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val shown = if (q.isEmpty()) categories
    else categories.filter { it.name.contains(q, ignoreCase = true) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text("Categoría", style = Kuodra.type.heading, color = c.ink,
            modifier = Modifier.padding(bottom = 8.dp))
        KuodraSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Buscar categoría…",
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        shown.forEach { cat ->
            val isSel = cat.id == selected.id
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
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Kuodra.shape.lg)
                .border(1.dp, c.line, Kuodra.shape.lg)
                .clickable(onClick = onCreate).padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(34.dp).clip(Kuodra.shape.md).background(c.surface2),
                contentAlignment = Alignment.Center) {
                PlusIcon(15.dp, c.primary)
            }
            Text("Crear categoría", style = Kuodra.type.body, color = c.primary, modifier = Modifier.weight(1f))
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
private fun DetailSheet(
    c: KuodraColors,
    items: List<MovementItem>,
    adjustment: String,
    onConcept: (String, String) -> Unit,
    onAmount: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text("Detalle", style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 4.dp))
        Text(
            "Desglosa el gasto en partidas. Lo no detallado queda como Ajuste.",
            style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(bottom = 10.dp),
        )

        items.forEach { item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Kuodra.shape.lg)
                    .background(c.surface).border(1.dp, c.line, Kuodra.shape.lg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = item.concept,
                    onValueChange = { onConcept(item.id, it) },
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
                        .clickable { onAmount(item.id) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(Calc.formatAmount(item.amount.major), style = Kuodra.type.heading, color = c.ink) }
                Box(
                    Modifier.size(28.dp).clip(Kuodra.shape.pill).background(c.negTint)
                        .clickable { onRemove(item.id) },
                    contentAlignment = Alignment.Center,
                ) { Text("×", style = Kuodra.type.heading, color = c.neg) }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Kuodra.shape.lg)
                .border(1.dp, c.line, Kuodra.shape.lg)
                .clickable(onClick = onAdd).padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlusIcon(14.dp, c.primary)
            Text("Añadir partida", style = Kuodra.type.body, color = c.primary)
        }

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
            Text(adjustment, style = Kuodra.type.heading, color = c.tintInk)
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
