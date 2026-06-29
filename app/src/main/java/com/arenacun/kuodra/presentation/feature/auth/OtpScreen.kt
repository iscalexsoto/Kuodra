package com.arenacun.kuodra.presentation.feature.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.theme.Kuodra

@Composable
fun OtpScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onChangeEmail: () -> Unit,
    onVerified: () -> Unit,
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.otpVerified.collect { onVerified() }
    }

    Column(Modifier.fillMaxSize().background(c.screenBg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackCircle(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("Cambiar correo", style = Kuodra.type.caption, color = c.primary,
                modifier = Modifier.clickable { onChangeEmail() })
        }
        Column(Modifier.weight(1f).padding(horizontal = 22.dp, vertical = 12.dp)) {
            Text("Revisa tu correo", style = Kuodra.type.titleScreen, color = c.ink)
            Text(
                "Enviamos un código de 6 dígitos a ${state.email.ifBlank { "tu correo" }}. " +
                    "Puede tardar un momento en llegar.",
                style = Kuodra.type.body, color = c.ink2,
                modifier = Modifier.padding(top = 9.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            ) {
                for (i in 0 until 6) {
                    val ch = state.otp.getOrNull(i)
                    val border = when {
                        state.otpError -> c.neg
                        ch != null -> c.primary
                        i == state.otp.length -> c.primary
                        else -> c.line
                    }
                    Box(
                        Modifier
                            .size(width = 42.dp, height = 54.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(c.surface)
                            .border(2.dp, border, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ch?.toString() ?: "", style = Kuodra.type.titleScreen,
                            color = if (state.otpError) c.neg else c.ink)
                    }
                }
            }
            Text(
                if (state.otpError) "Código incorrecto. Inténtalo de nuevo." else "",
                style = Kuodra.type.caption, color = c.neg,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 13.dp),
            )

            Spacer(Modifier.weight(1f))
            OtpKeypad(
                onDigit = viewModel::onOtpDigit,
                onBackspace = viewModel::onOtpBackspace,
            )
            Box(
                Modifier.fillMaxWidth().padding(top = 14.dp)
                    .clip(Kuodra.shape.md).background(c.tint).padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (state.verifying) "Verificando…" else "¿No te llegó? Revisa la carpeta de spam.",
                    style = Kuodra.type.caption, color = c.primary, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OtpKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val c = Kuodra.colors
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(Kuodra.shape.md)
                            .background(if (key.isEmpty()) c.screenBg else c.surface)
                            .then(
                                if (key.isEmpty()) Modifier
                                else Modifier.clickable {
                                    if (key == "⌫") onBackspace() else onDigit(key)
                                }
                            )
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key.isNotEmpty()) {
                            Text(key, style = Kuodra.type.heading, color = c.ink)
                        }
                    }
                }
            }
        }
    }
}
