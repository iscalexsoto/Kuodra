package com.arenacun.kuodra.presentation.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.presentation.theme.Kuodra
import org.koin.androidx.compose.koinViewModel

@Composable
fun NameScreen(
    onContinue: () -> Unit,
    viewModel: NameViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val enabled = state.valid && !state.saving

    LaunchedEffect(Unit) {
        viewModel.nameSaved.collect { onContinue() }
    }

    Column(
        Modifier.fillMaxSize().background(c.screenBg)
            .padding(horizontal = 22.dp, vertical = 28.dp),
    ) {
        Text("¿Cómo te llamas?", style = Kuodra.type.titleScreen, color = c.ink)
        Text(
            "Así te identificaremos en Kuodra. Podrás cambiarlo cuando quieras.",
            style = Kuodra.type.body, color = c.ink2,
            modifier = Modifier.padding(top = 10.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 30.dp)
                .clip(Kuodra.shape.lg)
                .background(c.surface)
                .border(2.dp, if (state.valid) c.primary else c.line, Kuodra.shape.lg)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text("NOMBRE", style = Kuodra.type.overline, color = c.primary)
            BasicTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                singleLine = true,
                textStyle = Kuodra.type.body.copy(color = c.ink),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                decorationBox = { inner ->
                    if (state.name.isEmpty()) {
                        Text("Tu nombre", style = Kuodra.type.body, color = c.ink3)
                    }
                    inner()
                },
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(Kuodra.shape.lg)
                .background(if (enabled) c.primary else c.line)
                .clickable(enabled = enabled) { viewModel.submit() }
                .padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (state.saving) "Guardando…" else "Continuar",
                style = Kuodra.type.heading,
                color = if (enabled) c.primaryInk else c.ink3,
            )
        }
        Text(
            state.error ?: "El botón se activa cuando escribes tu nombre.",
            style = Kuodra.type.caption,
            color = if (state.error != null) c.neg else c.ink3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}
