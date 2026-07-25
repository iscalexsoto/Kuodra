package com.arenacun.kuodra.presentation.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.R
import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import com.arenacun.kuodra.domain.model.Countries
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitRule
import com.arenacun.kuodra.domain.model.initialsOf
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.domain.model.ThemeMode
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.Chevron
import com.arenacun.kuodra.presentation.component.KIcon
import com.arenacun.kuodra.presentation.component.KuodraBottomSheet
import com.arenacun.kuodra.presentation.component.KuodraCalculator
import com.arenacun.kuodra.presentation.component.KuodraNumberPad
import com.arenacun.kuodra.presentation.component.KuodraTextField
import com.arenacun.kuodra.presentation.component.KuodraThemeSwitch
import com.arenacun.kuodra.presentation.component.PlusIcon
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.signedOut.collect { onSignedOut() }
    }
    LaunchedEffect(Unit) {
        viewModel.closed.collect { onBack() }
    }

    val settings = state.settings ?: return

    Column(
        Modifier.fillMaxSize().background(c.screenBg).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 2.dp, top = 6.dp, bottom = if (state.useCase == UseCase.Personal) 6.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = onBack)
            Text(titleLabel(state.useCase), style = Kuodra.type.heading, color = c.ink)
        }

        // Nombre (Gastos / Caja; en Personal el nombre es implícito)
        if (state.useCase != UseCase.Personal) {
            SectionLabel(c, nameLabel(state.useCase))
            Card(c) {
                BasicTextField(
                    value = settings.name,
                    onValueChange = viewModel::onNameChange,
                    singleLine = true,
                    textStyle = Kuodra.type.body.copy(color = c.ink),
                    cursorBrush = SolidColor(c.primary),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }
        }

        when (state.useCase) {
            UseCase.Personal -> {
                settings.budget?.let { BudgetSection(c, it, viewModel) }
            }
            UseCase.Gastos -> {
                MembersSection(c, "MIEMBROS", state.contacts, viewModel)
                settings.splitRule?.let { SplitRuleSection(c, it, state, viewModel) }
                ReminderRow(c, "Recordatorio de liquidación", settings.reminderEnabled, viewModel::onToggleReminder)
            }
        }

        // Categorías: pantalla dedicada con buscador (todos los casos de uso)
        SectionLabel(c, "CATEGORÍAS")
        Card(c) {
            NavRow(c, R.drawable.ic_categories, "Categorías", "Crea, edita y busca tus categorías", onOpenCategories)
        }

        // Apariencia (tema del sistema / claro / oscuro)
        SectionLabel(c, "APARIENCIA")
        Card(c) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SettingIcon(c, R.drawable.ic_sun_moon)
                    Column(Modifier.weight(1f)) {
                        Text("Tema", style = Kuodra.type.heading, color = c.ink)
                        Text(themeSubtitle(state.themeMode), style = Kuodra.type.caption, color = c.ink3)
                    }
                }
                KuodraThemeSwitch(selected = state.themeMode, onSelect = viewModel::onSetThemeMode)
            }
        }

        // Cuenta (nombre editable + correo de la sesión + cerrar sesión)
        SectionLabel(c, "CUENTA")
        Card(c) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = viewModel::onEditName)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Nombre", style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                    Text(
                        state.accountName.ifBlank { "Sin definir" },
                        style = Kuodra.type.caption,
                        color = if (state.accountName.isBlank()) c.ink3 else c.ink,
                    )
                    Box(Modifier.width(8.dp))
                    Chevron(7.dp, c.ink3, degrees = 0f)
                }
                Divider(c)
                viewModel.accountEmail?.let { email ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Correo", style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                        Text(email, style = Kuodra.type.caption, color = c.ink3)
                    }
                    Divider(c)
                }
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = viewModel::onSignOut)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Cerrar sesión", style = Kuodra.type.body, color = c.neg, modifier = Modifier.weight(1f))
                }
            }
        }

        // Archivar espacio (Gastos): lo guarda sin borrar y vuelve a Personal.
        if (state.useCase == UseCase.Gastos) {
            SectionLabel(c, "ESPACIO")
            Card(c) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = viewModel::onArchiveSpace)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Archivar espacio", style = Kuodra.type.body, color = c.neg, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // ===== Overlays =====
    if (state.calcTarget != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = viewModel::onDismissCalc) {
            KuodraCalculator(
                state = state.calc,
                title = "MONTO DEL PRESUPUESTO",
                confirmLabel = "Confirmar monto",
                onKey = viewModel::onCalcKey,
                onConfirm = viewModel::onConfirmCalc,
            )
        }
    }
    state.editingContact?.let { draft ->
        KuodraBottomSheet(onDismiss = viewModel::onCloseContact) {
            ContactSheet(c, draft, viewModel)
        }
    }
    state.editingName?.let { draft ->
        KuodraBottomSheet(onDismiss = viewModel::onCloseName) {
            NameSheet(c, draft, viewModel)
        }
    }
    // Porcentaje de un participante de la regla: number pad, nunca teclado del sistema.
    state.rulePadPersonId?.let { id ->
        Dialog(onDismissRequest = viewModel::onDismissRulePad) {
            KuodraNumberPad(
                state = state.rulePad,
                title = "PORCENTAJE DE ${state.ruleMemberName(id).uppercase()}",
                confirmLabel = "Confirmar",
                onKey = viewModel::onRulePadKey,
                onConfirm = viewModel::onConfirmRulePad,
            )
        }
    }
}

/** Hoja para editar el nombre del usuario (sección Cuenta). */
@Composable
private fun NameSheet(c: KuodraColors, draft: String, viewModel: SettingsViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text("Tu nombre", style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 12.dp))
        KuodraTextField(
            value = draft,
            onValueChange = viewModel::onNameDraftChange,
            label = "NOMBRE",
            placeholder = "¿Cómo te llamas?",
        )
        val canSave = draft.isNotBlank()
        Box(
            Modifier.fillMaxWidth().padding(top = 18.dp).clip(Kuodra.shape.lg)
                .background(if (canSave) c.primary else c.surface2)
                .clickable(enabled = canSave, onClick = viewModel::onConfirmName).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Guardar", style = Kuodra.type.heading, color = if (canSave) c.primaryInk else c.ink3) }
    }
}

/** Fila que navega a otra pantalla: etiqueta + descripción + chevron a la derecha. */
@Composable
private fun NavRow(c: KuodraColors, @DrawableRes icon: Int, label: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingIcon(c, icon)
        Column(Modifier.weight(1f)) {
            Text(label, style = Kuodra.type.heading, color = c.ink)
            if (sub.isNotEmpty()) {
                Text(sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Chevron(7.dp, c.ink3, degrees = 0f)
    }
}

/** Contenedor de ícono de ajuste: caja 42dp tint + glifo `ic_*` en tintInk (patrón de MenuOptionRow). */
@Composable
private fun SettingIcon(c: KuodraColors, @DrawableRes iconRes: Int) {
    Box(
        Modifier.size(42.dp).clip(Kuodra.shape.md).background(c.tint),
        contentAlignment = Alignment.Center,
    ) { KIcon(iconRes, 20.dp, c.tintInk) }
}

@Composable
private fun BudgetSection(c: KuodraColors, budget: BudgetConfig, viewModel: SettingsViewModel) {
    // Encabezado + contenido en una sola card (como el diseño). El toggle vive en el encabezado.
    Card(c, Modifier.padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SettingIcon(c, R.drawable.ic_credit_card)
                Column(Modifier.weight(1f)) {
                    Text("Presupuesto", style = Kuodra.type.heading, color = c.ink)
                    Text(
                        "Actívalo para definir frecuencia, días de pago y monto límite. El dashboard mostrará tu avance y ritmo de gasto.",
                        style = Kuodra.type.caption, color = c.ink3,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                KToggle(c, budget.enabled, viewModel::onToggleBudget)
            }

            if (budget.enabled) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp).height(1.dp).background(c.line))
                Text("FRECUENCIA", style = Kuodra.type.overline, color = c.ink3,
                    modifier = Modifier.padding(bottom = 11.dp))
                FreqGrid(c, budget.frequency, viewModel::onSetFrequency)

                when (budget.frequency) {
                    BudgetFrequency.Weekly -> WeekdayDetail(c, budget.weekday, viewModel)
                    BudgetFrequency.Biweekly -> BiweeklyDetail(c, budget, viewModel)
                    BudgetFrequency.Monthly -> MonthlyDetail(c, budget.monthlyDay, viewModel)
                    BudgetFrequency.Custom -> CustomDetail(c, budget.customInterval, viewModel)
                }

                AmountLimit(c, budget.amount) { viewModel.onOpenCalc(CalcTarget.Budget) }
            }
        }
    }

    if (budget.enabled) {
        // Nota: cierre manual desde el dashboard (fuera de la card, como el diseño).
        WarnNote(
            c,
            "El cierre manual del periodo está disponible desde el menú ··· del dashboard.",
            Modifier.padding(top = 12.dp),
        )
    }
}

/**
 * División por defecto (Gastos): el acuerdo fijo del espacio ("mi hermano paga el 75%"). Se aplica
 * como prellenado a cada gasto nuevo, así que no hay que capturar la división una por una.
 */
@Composable
private fun SplitRuleSection(
    c: KuodraColors,
    rule: SplitRule,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val members = state.ruleMembers
    Card(c, Modifier.padding(top = 12.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SettingIcon(c, R.drawable.ic_credit_card)
                Column(Modifier.weight(1f)) {
                    Text("División por defecto", style = Kuodra.type.heading, color = c.ink)
                    Text(
                        "Cada gasto nuevo llega con esta división ya puesta. Puedes cambiarla dentro del gasto cuando sea la excepción.",
                        style = Kuodra.type.caption, color = c.ink3,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                KToggle(c, rule.enabled, viewModel::onToggleSplitRule)
            }

            if (rule.enabled) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp).height(1.dp).background(c.line))
                Text("MODO", style = Kuodra.type.overline, color = c.ink3,
                    modifier = Modifier.padding(bottom = 11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FreqButton(c, "Equitativo", rule.mode == SplitMode.Equal, Modifier.weight(1f)) {
                        viewModel.onSetRuleMode(SplitMode.Equal)
                    }
                    FreqButton(c, "Porcentajes", rule.mode == SplitMode.Percent, Modifier.weight(1f)) {
                        viewModel.onSetRuleMode(SplitMode.Percent)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("PARTICIPANTES", style = Kuodra.type.overline, color = c.ink3,
                        modifier = Modifier.weight(1f))
                    state.splitRuleError?.let { Text(it, style = Kuodra.type.caption, color = c.neg) }
                }
                members.forEach { p ->
                    val participates = p.id in rule.participantIds
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Kuodra.shape.lg)
                            .background(if (participates) c.tint else c.surface2)
                            .border(1.dp, if (participates) c.primary else c.line, Kuodra.shape.lg)
                            .clickable { viewModel.onToggleRuleParticipant(p.id) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToneAvatar(initialsOf(p.name), toneForName(p.name), size = 32.dp)
                        Text(p.name, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                        when {
                            !participates -> Text("No participa", style = Kuodra.type.caption, color = c.ink3)
                            rule.mode == SplitMode.Percent -> Box(
                                Modifier.clip(Kuodra.shape.md).background(c.surface)
                                    .border(1.dp, c.line, Kuodra.shape.md)
                                    .clickable { viewModel.onOpenRulePad(p.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) { Text("${rule.percentOf(p.id)}%", style = Kuodra.type.heading, color = c.ink) }
                            else -> Text("Parte igual", style = Kuodra.type.caption, color = c.tintInk)
                        }
                    }
                }
                if (state.splitRuleError != null && rule.mode == SplitMode.Percent) {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 8.dp).clip(Kuodra.shape.md)
                            .border(1.dp, c.line, Kuodra.shape.md)
                            .clickable(onClick = viewModel::onDistributeRuleEvenly)
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Repartir parejo", style = Kuodra.type.body, color = c.primary) }
                }

                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp).height(1.dp).background(c.line))
                Text("QUIÉN SUELE PAGAR", style = Kuodra.type.overline, color = c.ink3,
                    modifier = Modifier.padding(bottom = 11.dp))
                MemberChipGrid(c, members, rule.payerId, viewModel::onSetRulePayer)

                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp).height(1.dp).background(c.line))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Registrar mi parte en Personal", style = Kuodra.type.body, color = c.ink)
                        Text(
                            "Al guardar un gasto compartido, tu parte se registra en Personal sin preguntar.",
                            style = Kuodra.type.caption, color = c.ink3,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    KToggle(c, rule.autoPersonalCopy, viewModel::onToggleRuleAutoPersonalCopy)
                }
            }
        }
    }
}

/** Chips de selección única sobre los miembros, 2 por fila (mismo patrón que [FreqGrid]). */
@Composable
private fun MemberChipGrid(
    c: KuodraColors,
    members: List<SpacePerson>,
    selectedId: String,
    onPick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        members.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { p ->
                    FreqButton(c, p.name, p.id == selectedId, Modifier.weight(1f)) { onPick(p.id) }
                }
                // Rellena la fila impar para que el último chip no ocupe el doble de ancho.
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

/** Grid 2×2 de frecuencias (Semanal/Quincenal · Mensual/Personalizado). */
@Composable
private fun FreqGrid(c: KuodraColors, selected: BudgetFrequency, onPick: (BudgetFrequency) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BudgetFrequency.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { f ->
                    FreqButton(c, f.label, f == selected, Modifier.weight(1f)) { onPick(f) }
                }
            }
        }
    }
}

@Composable
private fun FreqButton(c: KuodraColors, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(Kuodra.shape.md)
            .background(if (selected) c.tint else c.surface2)
            .border(1.dp, if (selected) c.primary else c.line, Kuodra.shape.md)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Kuodra.type.body, color = if (selected) c.tintInk else c.ink2) }
}

/** Bloque de detalle bajo el grid: separador + etiqueta centrada + contenido. */
@Composable
private fun DetailBlock(c: KuodraColors, label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Divider(c)
        Text(
            label, style = Kuodra.type.caption, color = c.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 13.dp),
        )
        content()
    }
}

/** Semanal: día de la semana en que empieza el periodo. */
@Composable
private fun WeekdayDetail(c: KuodraColors, weekday: Int, viewModel: SettingsViewModel) {
    DetailBlock(c, "El periodo empieza cada") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArrowBtn(c, left = true) { viewModel.onWeekdayDelta(-1) }
            Text(
                DateLabels.weekdayName(weekday), style = Kuodra.type.titleScreen, color = c.ink,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 140.dp),
            )
            ArrowBtn(c, left = false) { viewModel.onWeekdayDelta(1) }
        }
    }
}

/** Quincenal: primer y segundo día + aviso de meses cortos. */
@Composable
private fun BiweeklyDetail(c: KuodraColors, budget: BudgetConfig, viewModel: SettingsViewModel) {
    DetailBlock(c, "Recibo ingreso los días") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DayBox(c, "Primer día", budget.firstDay, { viewModel.onFirstDayDelta(-1) }, { viewModel.onFirstDayDelta(1) },
                Modifier.weight(1f))
            DayBox(c, "Segundo día", budget.secondDay, { viewModel.onSecondDayDelta(-1) }, { viewModel.onSecondDayDelta(1) },
                Modifier.weight(1f))
        }
        FebruaryWarning(c)
    }
}

/** Mensual: día del mes de ingreso. */
@Composable
private fun MonthlyDetail(c: KuodraColors, day: Int, viewModel: SettingsViewModel) {
    DetailBlock(c, "Recibo ingreso el día") {
        BigStepper(c, day.toString(), "de cada mes", { viewModel.onMonthlyDayDelta(-1) }, { viewModel.onMonthlyDayDelta(1) })
    }
}

/** Personalizado: intervalo en días. */
@Composable
private fun CustomDetail(c: KuodraColors, interval: Int, viewModel: SettingsViewModel) {
    DetailBlock(c, "Cierra el periodo cada") {
        BigStepper(c, interval.toString(), "días", { viewModel.onCustomIntervalDelta(-1) }, { viewModel.onCustomIntervalDelta(1) })
    }
}

/** Caja con etiqueta + stepper compacto (Primer/Segundo día). */
@Composable
private fun DayBox(c: KuodraColors, label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier) {
    Column(modifier.clip(Kuodra.shape.md).background(c.surface2).padding(12.dp)) {
        Text(label, style = Kuodra.type.caption, color = c.ink3, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            MiniBtn(c, minus = true, onClick = onMinus)
            Text(value.toString(), style = Kuodra.type.titleScreen.copy(fontSize = 26.sp), color = c.ink)
            MiniBtn(c, minus = false, onClick = onPlus)
        }
    }
}

/** Stepper grande (Mensual / Personalizado): botones 42dp, número 38sp + sublabel. */
@Composable
private fun BigStepper(c: KuodraColors, value: String, sub: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BigBtn(c, minus = true, onClick = onMinus)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 80.dp)) {
            Text(value, style = Kuodra.type.displayAmount.copy(fontSize = 38.sp, letterSpacing = (-1).sp), color = c.ink)
            Text(sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
        }
        BigBtn(c, minus = false, onClick = onPlus)
    }
}

/** MONTO LÍMITE: monto grande + lápiz, abre la calculadora. */
@Composable
private fun AmountLimit(c: KuodraColors, amount: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Divider(c)
        Text("MONTO LÍMITE", style = Kuodra.type.overline, color = c.ink3,
            modifier = Modifier.padding(top = 18.dp, bottom = 13.dp))
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(amount, style = Kuodra.type.displayAmount.copy(fontSize = 42.sp), color = c.ink)
            Box(
                Modifier.size(30.dp).clip(Kuodra.shape.sm).background(c.surface2).border(1.dp, c.line, Kuodra.shape.sm),
                contentAlignment = Alignment.Center,
            ) { PencilIcon(13.dp, c.ink3) }
        }
        Text("por periodo · toca para editar", style = Kuodra.type.caption, color = c.ink3,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

/** Aviso de meses cortos (febrero) del modo Quincenal. */
@Composable
private fun FebruaryWarning(c: KuodraColors) {
    WarnNote(
        c,
        "En meses cortos (como febrero) puede que el segundo día no exista. Si pasa, cierra el periodo manualmente desde el dashboard.",
        Modifier.padding(top = 13.dp),
    )
}

/** Nota de advertencia (acento `warn`): tarjeta tintada + badge "!" + texto. Reutilizable. */
@Composable
private fun WarnNote(c: KuodraColors, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(Kuodra.shape.md).background(c.warnTint)
            .border(1.dp, c.warn.copy(alpha = 0.35f), Kuodra.shape.md)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(18.dp).clip(Kuodra.shape.pill).background(c.warn), contentAlignment = Alignment.Center) {
            Text("!", style = Kuodra.type.overline.copy(letterSpacing = 0.sp), color = Color.White)
        }
        Text(text, style = Kuodra.type.caption, color = c.ink2)
    }
}

/** Subtítulo del bloque de tema, según el modo activo. */
private fun themeSubtitle(mode: ThemeMode): String = when (mode) {
    ThemeMode.System -> "Sigue el tema del sistema"
    ThemeMode.Light -> "Siempre claro"
    ThemeMode.Dark -> "Siempre oscuro"
}

@Composable
private fun ArrowBtn(c: KuodraColors, left: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(Kuodra.shape.md).background(c.surface2).border(1.dp, c.line, Kuodra.shape.md)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Chevron(10.dp, c.ink2, degrees = if (left) 180f else 0f) }
}

@Composable
private fun MiniBtn(c: KuodraColors, minus: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(Kuodra.shape.sm).background(c.surface).border(1.dp, c.line, Kuodra.shape.sm)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { if (minus) MinusBar(12.dp, c.ink2) else PlusIcon(12.dp, c.ink2) }
}

@Composable
private fun BigBtn(c: KuodraColors, minus: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(Kuodra.shape.md).background(c.surface2).border(1.dp, c.line, Kuodra.shape.md)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { if (minus) MinusBar(14.dp, c.ink2) else PlusIcon(14.dp, c.ink2) }
}

/** Barra horizontal redondeada (signo menos). */
@Composable
private fun MinusBar(width: androidx.compose.ui.unit.Dp, color: Color) {
    Box(Modifier.width(width).height(2.5.dp).clip(Kuodra.shape.pill).background(color))
}

/** Ícono de lápiz (editar), réplica del SVG del prototipo. */
@Composable
private fun PencilIcon(size: androidx.compose.ui.unit.Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 13f
        val path = Path().apply {
            moveTo(9f * s, 1.5f * s)
            lineTo(11.5f * s, 4f * s)
            lineTo(4.5f * s, 11f * s)
            lineTo(2f * s, 11f * s)
            lineTo(2f * s, 8.5f * s)
            close()
        }
        drawPath(path, color, style = Stroke(width = 1.6f * s))
    }
}

@Composable
private fun MembersSection(c: KuodraColors, label: String, members: List<SpacePerson>, viewModel: SettingsViewModel) {
    SectionLabel(c, label)
    Card(c) {
        Column(Modifier.fillMaxWidth()) {
            members.forEach { p ->
                Row(
                    Modifier.fillMaxWidth().clickable { viewModel.onEditContact(p) }
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToneAvatar(initialsOf(p.name), toneForName(p.name), size = 36.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = Kuodra.type.body, color = c.ink)
                        Text(p.phone.ifBlank { "Sin WhatsApp" }, style = Kuodra.type.caption, color = c.ink3,
                            modifier = Modifier.padding(top = 1.dp))
                    }
                    Chevron(7.dp, c.ink3, degrees = 90f)
                }
                Divider(c)
            }
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.onAddContact() }
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(Kuodra.shape.pill).background(c.tint), contentAlignment = Alignment.Center) {
                    PlusIcon(16.dp, c.tintInk)
                }
                Text("Agregar contacto", style = Kuodra.type.body, color = c.tintInk)
            }
        }
    }
}

@Composable
private fun ReminderRow(c: KuodraColors, label: String, enabled: Boolean, onToggle: () -> Unit) {
    SectionLabel(c, "RECORDATORIOS")
    Card(c) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
            KToggle(c, enabled, onToggle)
        }
    }
}

@Composable
private fun ContactSheet(c: KuodraColors, draft: ContactDraft, viewModel: SettingsViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text(if (draft.id == null) "Agregar contacto" else "Editar contacto",
            style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 12.dp))
        SheetField(c, "NOMBRE", draft.name, "Nombre del contacto", viewModel::onContactName)
        Box(Modifier.height(10.dp))
        PhoneField(c, draft, viewModel)
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (draft.id != null) {
                Box(
                    Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.negTint)
                        .clickable(onClick = viewModel::onDeleteContact).padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KIcon(R.drawable.ic_delete, 16.dp, c.neg)
                        Text("Eliminar", style = Kuodra.type.heading, color = c.neg)
                    }
                }
            }
            Box(
                Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.primary)
                    .clickable(onClick = viewModel::onSaveContact).padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Guardar", style = Kuodra.type.heading, color = c.primaryInk) }
        }
    }
}

/** Campo de teléfono: chip de país (bandera + código, abre el selector) + número local. */
@Composable
private fun PhoneField(c: KuodraColors, draft: ContactDraft, viewModel: SettingsViewModel) {
    val country = Countries.byDialCode(draft.dialCode)
    Column(Modifier.fillMaxWidth()) {
        Text("WHATSAPP (OPCIONAL)", style = Kuodra.type.overline, color = c.ink3,
            modifier = Modifier.padding(bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(Kuodra.shape.lg).background(c.surface2).border(1.dp, c.line, Kuodra.shape.lg)
                    .clickable(onClick = viewModel::onOpenCountryPicker).padding(horizontal = 12.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(country.flag, style = Kuodra.type.body)
                Text(country.dialCode, style = Kuodra.type.body, color = c.ink)
                Chevron(7.dp, c.ink3, degrees = 90f)
            }
            Box(
                Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.surface2)
                    .border(1.dp, c.line, Kuodra.shape.lg).padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                BasicTextField(
                    value = draft.whatsapp,
                    onValueChange = viewModel::onContactWhatsapp,
                    singleLine = true,
                    textStyle = Kuodra.type.body.copy(color = c.ink),
                    cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (draft.whatsapp.isEmpty()) Text("Número", style = Kuodra.type.body, color = c.ink3)
                        inner()
                    },
                )
            }
        }
    }

    if (draft.countryPickerOpen) {
        Dialog(onDismissRequest = viewModel::onCloseCountryPicker) {
            Column(
                Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface).padding(vertical = 12.dp),
            ) {
                Text("Código de país", style = Kuodra.type.heading, color = c.ink,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                Countries.ALL.forEach { country ->
                    Row(
                        Modifier.fillMaxWidth().clickable { viewModel.onPickCountry(country.dialCode) }
                            .padding(horizontal = 18.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(country.flag, style = Kuodra.type.body)
                        Text(country.name, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                        Text(country.dialCode, style = Kuodra.type.body, color = c.ink3)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetField(c: KuodraColors, label: String, value: String, hint: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = Kuodra.type.overline, color = c.ink3, modifier = Modifier.padding(bottom = 6.dp))
        Box(Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface2)
            .border(1.dp, c.line, Kuodra.shape.lg).padding(horizontal = 14.dp, vertical = 13.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Kuodra.type.body.copy(color = c.ink),
                cursorBrush = SolidColor(c.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(hint, style = Kuodra.type.body, color = c.ink3)
                    inner()
                },
            )
        }
    }
}

// ---- bloques base ----

@Composable
private fun SectionLabel(c: KuodraColors, text: String) {
    Text(text, style = Kuodra.type.overline, color = c.ink3,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp))
}

@Composable
private fun Card(c: KuodraColors, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface).border(1.dp, c.line, Kuodra.shape.xl)) {
        content()
    }
}

@Composable
private fun KToggle(c: KuodraColors, on: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier.width(46.dp).height(28.dp).clip(Kuodra.shape.pill)
            .background(if (on) c.primary else c.surface2)
            .border(1.dp, if (on) c.primary else c.line, Kuodra.shape.pill)
            .clickable(onClick = onToggle),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.padding(horizontal = 3.dp).size(22.dp).clip(Kuodra.shape.pill)
            .background(if (on) c.primaryInk else c.surface))
    }
}

@Composable
private fun Divider(c: KuodraColors) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
}

private fun nameLabel(useCase: UseCase): String = when (useCase) {
    UseCase.Personal -> "NOMBRE"
    UseCase.Gastos -> "NOMBRE DEL GRUPO"
}

private fun titleLabel(useCase: UseCase): String = when (useCase) {
    UseCase.Personal -> "Ajustes"
    UseCase.Gastos -> "Ajustes del grupo"
}
