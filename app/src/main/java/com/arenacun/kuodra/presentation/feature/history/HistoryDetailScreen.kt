package com.arenacun.kuodra.presentation.feature.history

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.KuodraBottomSheet
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun HistoryDetailScreen(
    recordId: String,
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = koinViewModel { parametersOf(recordId) },
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val record = state.record

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
            Text("Detalle del corte", style = Kuodra.type.heading, color = c.ink)
        }

        if (record == null) {
            Text("Corte no encontrado", style = Kuodra.type.body, color = c.ink2)
            return@Column
        }

        // hero
        Column(
            Modifier.fillMaxWidth().clip(Kuodra.shape.xxl).background(c.primary).padding(22.dp),
        ) {
            Text(record.title, style = Kuodra.type.caption, color = c.primaryInk.copy(alpha = 0.85f))
            Text(record.total, style = Kuodra.type.displayAmount, color = c.primaryInk,
                modifier = Modifier.padding(top = 6.dp))
            Row(
                Modifier.padding(top = 12.dp).clip(Kuodra.shape.pill).background(c.primaryInk.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text("${record.periodLabel} · ${record.statLabel}", style = Kuodra.type.caption, color = c.primaryInk) }
        }

        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface).border(1.dp, c.line, Kuodra.shape.xl),
        ) {
            record.lines.forEachIndexed { i, line ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(line.name, style = Kuodra.type.body, color = c.ink)
                        Text(line.detail, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
                    }
                    Text(line.amount, style = Kuodra.type.heading,
                        color = when (line.positive) { true -> c.pos; false -> c.neg; null -> c.ink })
                }
                if (i < record.lines.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            }
        }

        Spacer(Modifier.height(22.dp))
        Box(
            Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface).border(1.5.dp, c.line, Kuodra.shape.lg)
                .clickable { viewModel.onReshare() }.padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Reenviar corte", style = Kuodra.type.heading, color = c.ink) }
    }

    when (state.sheet) {
        ReshareSheet.Options -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            ReshareOptions(c, viewModel)
        }
        ReshareSheet.Shared -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            SharedConfirmation(c, viewModel)
        }
        ReshareSheet.None -> {}
    }
}

@Composable
private fun ReshareOptions(c: KuodraColors, viewModel: HistoryDetailViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text("Reenviar corte", style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 12.dp))
        ReshareButton(c, "Compartir PDF", viewModel::onShare)
        Spacer(Modifier.height(10.dp))
        ReshareButton(c, "Enviar por WhatsApp", viewModel::onShare)
    }
}

@Composable
private fun ReshareButton(c: KuodraColors, label: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface2).border(1.dp, c.line, Kuodra.shape.lg)
            .clickable(onClick = onClick).padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Kuodra.type.heading, color = c.ink) }
}

@Composable
private fun SharedConfirmation(c: KuodraColors, viewModel: HistoryDetailViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).clip(Kuodra.shape.pill).background(c.posTint), contentAlignment = Alignment.Center) {
            Text("✓", style = Kuodra.type.displayAmount, color = c.pos)
        }
        Text("Corte enviado", style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(top = 12.dp))
        Text("Se compartió el resumen del periodo.", style = Kuodra.type.caption, color = c.ink3,
            modifier = Modifier.padding(top = 4.dp))
        Box(
            Modifier.fillMaxWidth().padding(top = 18.dp).clip(Kuodra.shape.lg).background(c.primary)
                .clickable(onClick = viewModel::onCloseSheet).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Entendido", style = Kuodra.type.heading, color = c.primaryInk) }
    }
}
