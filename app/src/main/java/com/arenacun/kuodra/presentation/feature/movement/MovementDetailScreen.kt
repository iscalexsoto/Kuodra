package com.arenacun.kuodra.presentation.feature.movement

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.CategoryTag
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MovementDetailScreen(
    movementId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: MovementDetailViewModel = koinViewModel { parametersOf(movementId) },
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val m = state.movement

    LaunchedEffect(Unit) {
        viewModel.deleted.collect { onDeleted() }
    }

    Column(
        Modifier.fillMaxSize().background(c.screenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 2.dp, top = 6.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = onBack)
            Text("Detalle del movimiento", style = Kuodra.type.heading, color = c.ink)
        }

        if (state.loading) return@Column
        if (m == null) {
            Text("Movimiento no encontrado", style = Kuodra.type.body, color = c.ink2)
            return@Column
        }

        // hero
        Column(
            Modifier.fillMaxWidth().clip(Kuodra.shape.xxl).background(c.surface)
                .border(1.dp, c.line, Kuodra.shape.xxl)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CategoryTag(m.catTag, m.tone, size = 52.dp)
            Text(m.amount, style = Kuodra.type.displayAmount, color = c.ink,
                modifier = Modifier.padding(top = 14.dp))
            Text(m.title, style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(top = 12.dp))
            Row(
                Modifier.padding(top = 11.dp).clip(Kuodra.shape.pill).background(c.surface2)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(m.catName, style = Kuodra.type.caption, color = c.ink2)
                Box(Modifier.size(3.dp).clip(Kuodra.shape.pill).background(c.ink3))
                Text(m.dateStr, style = Kuodra.type.caption, color = c.ink2)
            }
        }

        // meta
        val hasBy = m.by != null
        val splits = m.splitShares
        val hasNote = m.note.isNotBlank()
        if (hasBy || splits.isNotEmpty() || hasNote) {
            Column(
                Modifier.fillMaxWidth().padding(top = 14.dp).clip(Kuodra.shape.xl)
                    .background(c.surface).border(1.dp, c.line, Kuodra.shape.xl),
            ) {
                if (hasBy) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToneAvatar(if (m.by == "Tú") "T" else m.by!!.take(1), toneForName(m.by!!), size = 38.dp)
                        Column(Modifier.weight(1f)) {
                            Text((m.byVerb ?: "").uppercase(), style = Kuodra.type.overline, color = c.ink3)
                            Text(m.by!!, style = Kuodra.type.heading, color = c.ink,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    if (splits.isNotEmpty() || hasNote) MetaDivider(c)
                }
                if (splits.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("DIVIDIDO ENTRE ${splits.size}", style = Kuodra.type.overline, color = c.ink3)
                            Text("${m.perHead} c/u", style = Kuodra.type.caption, color = c.ink3)
                        }
                        splits.forEachIndexed { i, s ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ToneAvatar(s.initials, s.tone, size = 32.dp)
                                Text(s.name, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                                Text(s.share, style = Kuodra.type.heading, color = c.ink2)
                            }
                            if (i < splits.lastIndex) MetaDivider(c)
                        }
                    }
                    if (hasNote) MetaDivider(c)
                }
                if (hasNote) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("NOTA", style = Kuodra.type.overline, color = c.ink3)
                        Text(m.note, style = Kuodra.type.body, color = c.ink2, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }

        // actions
        Spacer(Modifier.height(18.dp))
        if (!state.confirmDelete) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.surface)
                        .border(1.5.dp, c.line, Kuodra.shape.lg)
                        .clickable { /* editar — fuera de alcance de la maqueta */ }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Editar", style = Kuodra.type.heading, color = c.ink) }
                Box(
                    Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.negTint)
                        .clickable { viewModel.onDeleteRequest() }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Eliminar", style = Kuodra.type.heading, color = c.neg) }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface)
                    .border(1.dp, c.neg, Kuodra.shape.xl).padding(18.dp),
            ) {
                Text("¿Eliminar este movimiento?", style = Kuodra.type.heading, color = c.ink)
                Text(
                    "Deja de contar en los balances y en el corte del periodo. No se puede deshacer.",
                    style = Kuodra.type.caption, color = c.ink2, modifier = Modifier.padding(top = 6.dp),
                )
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).clip(Kuodra.shape.lg).border(1.5.dp, c.line, Kuodra.shape.lg)
                            .clickable { viewModel.onCancelDelete() }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Cancelar", style = Kuodra.type.heading, color = c.ink2) }
                    Box(
                        Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.neg)
                            .clickable { viewModel.onConfirmDelete() }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Eliminar", style = Kuodra.type.heading, color = c.primaryInk) }
                }
            }
        }
    }
}

@Composable
private fun MetaDivider(c: KuodraColors) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
}
