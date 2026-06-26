package com.arenacun.kuodra.presentation.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.Chevron
import com.arenacun.kuodra.presentation.component.KuodraBottomSheet
import com.arenacun.kuodra.presentation.component.KuodraCalculator
import com.arenacun.kuodra.presentation.component.PlusIcon
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings ?: return

    Column(
        Modifier.fillMaxSize().background(c.screenBg).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 2.dp, top = 6.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = onBack)
            Text("Ajustes", style = Kuodra.type.heading, color = c.ink)
        }

        // Nombre
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

        when (state.useCase) {
            UseCase.Personal -> settings.budget?.let { BudgetSection(c, it, viewModel) }
            UseCase.Gastos -> {
                MembersSection(c, "MIEMBROS", settings.members, viewModel)
                ReminderRow(c, "Recordatorio de liquidación", settings.reminderEnabled, viewModel::onToggleReminder)
            }
            UseCase.Caja -> {
                SectionLabel(c, "FONDO INICIAL")
                Card(c) {
                    TapRow(c, "Monto inicial", settings.fund?.initial ?: "$0") {
                        viewModel.onOpenCalc(CalcTarget.Fund)
                    }
                }
                MembersSection(c, "AUTORIZADOS", settings.members, viewModel)
                ReminderRow(c, "Recordatorio de reposición", settings.reminderEnabled, viewModel::onToggleReminder)
            }
        }

        // Historial de cortes / periodos
        SectionLabel(c, "HISTORIAL")
        Card(c) {
            TapRow(c, if (state.useCase == UseCase.Personal) "Periodos cerrados" else "Cortes anteriores", "") {
                onOpenHistory()
            }
        }

        // Apariencia (control de tema del prototipo)
        SectionLabel(c, "APARIENCIA")
        Card(c) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tema oscuro", style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                KToggle(c, state.darkTheme, viewModel::onToggleTheme)
            }
        }
    }

    // ===== Overlays =====
    if (state.calcTarget != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = viewModel::onDismissCalc) {
            KuodraCalculator(
                state = state.calc,
                title = if (state.calcTarget == CalcTarget.Budget) "MONTO DEL PRESUPUESTO" else "MONTO DEL FONDO",
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
}

@Composable
private fun BudgetSection(c: KuodraColors, budget: BudgetConfig, viewModel: SettingsViewModel) {
    SectionLabel(c, "PRESUPUESTO")
    Card(c) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Activar presupuesto", style = Kuodra.type.body, color = c.ink)
                    Text("Define un límite por periodo", style = Kuodra.type.caption, color = c.ink3,
                        modifier = Modifier.padding(top = 1.dp))
                }
                KToggle(c, budget.enabled, viewModel::onToggleBudget)
            }
            if (budget.enabled) {
                Divider(c)
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("FRECUENCIA", style = Kuodra.type.overline, color = c.ink3,
                        modifier = Modifier.padding(bottom = 8.dp))
                    FreqChips(c, budget.frequency, viewModel::onSetFrequency)
                }
                Divider(c)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Día de cierre", style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                    Stepper(c, budget.closingDay, { viewModel.onClosingDayDelta(-1) }, { viewModel.onClosingDayDelta(1) })
                }
                Divider(c)
                TapRow(c, "Monto límite", budget.amount) { viewModel.onOpenCalc(CalcTarget.Budget) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FreqChips(c: KuodraColors, selected: BudgetFrequency, onPick: (BudgetFrequency) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BudgetFrequency.entries.forEach { f ->
            val isSel = f == selected
            Box(
                Modifier.clip(Kuodra.shape.pill)
                    .background(if (isSel) c.tint else c.surface2)
                    .border(1.dp, if (isSel) c.primary else c.line, Kuodra.shape.pill)
                    .clickable { onPick(f) }.padding(horizontal = 14.dp, vertical = 9.dp),
            ) { Text(f.label, style = Kuodra.type.caption, color = if (isSel) c.tintInk else c.ink2) }
        }
    }
}

@Composable
private fun MembersSection(c: KuodraColors, label: String, members: List<Person>, viewModel: SettingsViewModel) {
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
                    ToneAvatar(p.initials, p.tone, size = 36.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = Kuodra.type.body, color = c.ink)
                        Text(p.sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
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
                    PlusIcon(16.dp, c.tintInk, thickness = 2.5.dp)
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
        Text(if (draft.original == null) "Agregar contacto" else "Editar contacto",
            style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 12.dp))
        SheetField(c, "NOMBRE", draft.name, "Nombre del contacto", viewModel::onContactName)
        Box(Modifier.height(10.dp))
        SheetField(c, "WHATSAPP (OPCIONAL)", draft.whatsapp, "+52 …", viewModel::onContactWhatsapp)
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (draft.original != null) {
                Box(
                    Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.negTint)
                        .clickable(onClick = viewModel::onDeleteContact).padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Eliminar", style = Kuodra.type.heading, color = c.neg) }
            }
            Box(
                Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.primary)
                    .clickable(onClick = viewModel::onSaveContact).padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Guardar", style = Kuodra.type.heading, color = c.primaryInk) }
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
private fun Card(c: KuodraColors, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface).border(1.dp, c.line, Kuodra.shape.xl)) {
        content()
    }
}

@Composable
private fun TapRow(c: KuodraColors, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
        Text(value, style = Kuodra.type.heading, color = c.ink)
        Box(Modifier.width(8.dp))
        Chevron(7.dp, c.ink3, degrees = 90f)
    }
}

@Composable
private fun Stepper(c: KuodraColors, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepBtn(c, "−", onMinus)
        Text(value.toString(), style = Kuodra.type.heading, color = c.ink)
        StepBtn(c, "+", onPlus)
    }
}

@Composable
private fun StepBtn(c: KuodraColors, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(Kuodra.shape.pill).background(c.surface2)
            .border(1.dp, c.line, Kuodra.shape.pill).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Kuodra.type.heading, color = c.ink2) }
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
    UseCase.Caja -> "NOMBRE DEL FONDO"
}
