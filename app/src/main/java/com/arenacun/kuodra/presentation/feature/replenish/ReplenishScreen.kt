package com.arenacun.kuodra.presentation.feature.replenish

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.KuodraCalculator
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReplenishScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ReplenishViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.done.collect { onDone() } }

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
            Text("Reponer fondo", style = Kuodra.type.heading, color = c.ink)
        }

        // stats
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(c, "Saldo actual", state.current, Modifier.weight(1f))
            StatCard(c, "Fondo inicial", state.initial, Modifier.weight(1f))
        }

        // sugerencia
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp).clip(Kuodra.shape.lg).background(c.posTint)
                .clickable { viewModel.onUseSuggested() }.padding(horizontal = 15.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sugerido para llenar el fondo", style = Kuodra.type.caption, color = c.ink)
                Text("Toca para usar ${state.suggested}", style = Kuodra.type.caption, color = c.ink3,
                    modifier = Modifier.padding(top = 1.dp))
            }
            Text(state.suggested, style = Kuodra.type.heading, color = c.pos)
        }

        // monto a reponer
        Column(
            Modifier.fillMaxWidth().padding(top = 14.dp).clip(Kuodra.shape.xl).background(c.tint)
                .border(1.5.dp, c.primary, Kuodra.shape.xl)
                .clickable { viewModel.onOpenCalculator() }.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("MONTO A REPONER →", style = Kuodra.type.overline, color = c.tintInk)
            Text(state.amountLabel, style = Kuodra.type.displayAmount,
                color = if (state.hasAmount) c.ink else c.ink3, modifier = Modifier.padding(top = 2.dp))
        }

        // nota
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp).clip(Kuodra.shape.xl).background(c.surface)
                .border(1.dp, c.line, Kuodra.shape.xl).padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text("Nota (opcional)", style = Kuodra.type.caption, color = c.ink3)
            BasicTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                singleLine = true,
                textStyle = Kuodra.type.body.copy(color = c.ink),
                cursorBrush = SolidColor(c.primary),
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                decorationBox = { inner ->
                    if (state.note.isEmpty()) Text("¿De dónde sale la reposición?", style = Kuodra.type.body, color = c.ink3)
                    inner()
                },
            )
        }

        Spacer(Modifier.height(22.dp))
        Box(
            Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.primary)
                .clickable { viewModel.onRegister() }.padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Registrar reposición", style = Kuodra.type.heading, color = c.primaryInk) }
    }

    if (state.showCalculator) {
        Dialog(onDismissRequest = viewModel::onDismissCalculator) {
            KuodraCalculator(
                state = state.calc,
                title = "MONTO A REPONER",
                confirmLabel = "Confirmar monto",
                onKey = viewModel::onCalcKey,
                onConfirm = viewModel::onConfirmAmount,
            )
        }
    }
}

@Composable
private fun StatCard(c: KuodraColors, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(Kuodra.shape.xl).background(c.surface).border(1.dp, c.line, Kuodra.shape.xl)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, style = Kuodra.type.caption, color = c.ink3)
        Text(value, style = Kuodra.type.titleScreen, color = c.ink, modifier = Modifier.padding(top = 4.dp))
    }
}
