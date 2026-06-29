package com.arenacun.kuodra.presentation.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.theme.Kuodra
import org.koin.androidx.compose.koinViewModel

@Composable
fun EmailScreen(
    onBack: () -> Unit,
    onCodeSent: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val valid = state.emailValid
    val enabled = valid && !state.requesting

    LaunchedEffect(Unit) {
        viewModel.otpSent.collect { onCodeSent() }
    }

    Column(
        Modifier.fillMaxSize().background(c.screenBg),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackCircle(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("Otras opciones", style = Kuodra.type.caption, color = c.primary,
                modifier = Modifier.clickable { onBack() })
        }
        Column(Modifier.weight(1f).padding(horizontal = 22.dp, vertical = 12.dp)) {
            Text("¿Cuál es tu correo?", style = Kuodra.type.titleScreen, color = c.ink)
            Text(
                "Te enviaremos un código para confirmar tu identidad. Sin contraseñas.",
                style = Kuodra.type.body, color = c.ink2,
                modifier = Modifier.padding(top = 10.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp)
                    .clip(Kuodra.shape.lg)
                    .background(c.surface)
                    .border(2.dp, if (valid) c.primary else c.line, Kuodra.shape.lg)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text("CORREO ELECTRÓNICO", style = Kuodra.type.overline, color = c.primary)
                BasicTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    singleLine = true,
                    textStyle = Kuodra.type.body.copy(color = c.ink),
                    cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    decorationBox = { inner ->
                        if (state.email.isEmpty()) {
                            Text("nombre@correo.com", style = Kuodra.type.body, color = c.ink3)
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
                    .clickable(enabled = enabled) { viewModel.requestOtp() }
                    .padding(vertical = 17.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (state.requesting) "Enviando…" else "Enviar código",
                    style = Kuodra.type.heading,
                    color = if (enabled) c.primaryInk else c.ink3,
                )
            }
            Text(
                state.requestError ?: "El botón se activa cuando el correo tiene un formato válido.",
                style = Kuodra.type.caption,
                color = if (state.requestError != null) c.neg else c.ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}
