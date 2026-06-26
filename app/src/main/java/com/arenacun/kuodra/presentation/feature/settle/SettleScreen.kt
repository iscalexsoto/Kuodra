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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.presentation.component.BackCircle
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
            state.people.forEachIndexed { i, p ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
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
                if (i < state.people.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            }
        }

        Spacer(Modifier.height(22.dp))
        Box(
            Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.primary)
                .clickable { viewModel.onRegister() }.padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) { Text(state.confirmLabel, style = Kuodra.type.heading, color = c.primaryInk) }
    }
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
