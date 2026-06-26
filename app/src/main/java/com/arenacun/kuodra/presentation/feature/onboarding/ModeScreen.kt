package com.arenacun.kuodra.presentation.feature.onboarding

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.presentation.component.avatarBg
import com.arenacun.kuodra.presentation.component.avatarInk
import com.arenacun.kuodra.presentation.theme.Kuodra
import org.koin.androidx.compose.koinViewModel

@Composable
fun ModeScreen(
    onContinueToCreate: (UseCase) -> Unit,
    onContinueToApp: () -> Unit,
    viewModel: ModeViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(c.screenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
    ) {
        Text("Bienvenido a Kuodra,\nDiego", style = Kuodra.type.titleScreen, color = c.ink)
        Text(
            "¿Qué quieres configurar además de tus gastos personales?",
            style = Kuodra.type.body, color = c.ink2,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            Modifier.padding(top = 12.dp).clip(Kuodra.shape.md).background(c.tint)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                "Tu espacio Personal ya está listo. Puedes agregar más cuando quieras — no es permanente.",
                style = Kuodra.type.caption, color = c.tintInk,
            )
        }

        Spacer(Modifier.padding(top = 12.dp))
        Column(
            Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeCard(selectedMode, UseCase.Personal, "Solo personal por ahora",
                "Empieza con tus gastos, agrega lo demás después", AvatarTone.Tint, "Yo", viewModel::onSelect)
            ModeCard(selectedMode, UseCase.Gastos, "Grupo compartido",
                "Divide gastos con roomies, pareja o familia", AvatarTone.Pos, "○○", viewModel::onSelect)
            ModeCard(selectedMode, UseCase.Caja, "Caja chica",
                "Maneja el fondo de efectivo de tu negocio", AvatarTone.Warn, "$", viewModel::onSelect)
        }

        val chosen = selectedMode != null
        Box(
            Modifier.padding(top = 18.dp).fillMaxWidth()
                .clip(Kuodra.shape.lg)
                .background(if (chosen) c.primary else c.line)
                .clickable(enabled = chosen) {
                    when (selectedMode) {
                        UseCase.Personal -> {
                            viewModel.selectPersonal()
                            onContinueToApp()
                        }
                        null -> {}
                        else -> onContinueToCreate(selectedMode!!)
                    }
                }
                .padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Continuar", style = Kuodra.type.heading,
                color = if (chosen) c.primaryInk else c.ink3)
        }
    }
}

@Composable
private fun ModeCard(
    selectedMode: UseCase?,
    mode: UseCase,
    title: String,
    sub: String,
    tone: AvatarTone,
    glyph: String,
    onSelect: (UseCase) -> Unit,
) {
    val c = Kuodra.colors
    val selected = selectedMode == mode
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Kuodra.shape.xl)
            .background(if (selected) c.tint else c.surface)
            .border(1.5.dp, if (selected) c.primary else c.line, Kuodra.shape.xl)
            .clickable { onSelect(mode) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(Kuodra.shape.md)
                .background(Kuodra.colors.avatarBg(tone)),
            contentAlignment = Alignment.Center,
        ) { Text(glyph, style = Kuodra.type.heading, color = Kuodra.colors.avatarInk(tone)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = Kuodra.type.heading, color = c.ink)
            Text(sub, style = Kuodra.type.caption, color = c.ink3,
                modifier = Modifier.padding(top = 2.dp))
        }
        Box(
            Modifier.size(22.dp).clip(Kuodra.shape.pill)
                .border(2.dp, if (selected) c.primary else c.line, Kuodra.shape.pill),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(11.dp).clip(Kuodra.shape.pill)
                    .background(if (selected) c.primary else Color.Transparent)
            )
        }
    }
}
