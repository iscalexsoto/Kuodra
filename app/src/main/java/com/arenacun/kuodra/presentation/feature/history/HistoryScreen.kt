package com.arenacun.kuodra.presentation.feature.history

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.Chevron
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val c = Kuodra.colors

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
            Text("Historial de cortes", style = Kuodra.type.heading, color = c.ink)
        }

        if (viewModel.records.isEmpty()) {
            Text("Aún no hay periodos cerrados", style = Kuodra.type.body, color = c.ink3,
                modifier = Modifier.padding(top = 24.dp))
        }
        viewModel.records.forEach { record -> RecordCard(c, record, onOpen) }
    }
}

@Composable
private fun RecordCard(c: KuodraColors, record: SettlementRecord, onOpen: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(Kuodra.shape.xl).background(c.surface)
            .border(1.dp, c.line, Kuodra.shape.xl).clickable { onOpen(record.id) }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(record.title, style = Kuodra.type.body, color = c.ink)
            Text("${record.periodLabel} · ${record.statLabel}", style = Kuodra.type.caption, color = c.ink3,
                modifier = Modifier.padding(top = 2.dp))
        }
        Text(record.total, style = Kuodra.type.heading, color = c.ink)
        Chevron(7.dp, c.ink3)
    }
}
