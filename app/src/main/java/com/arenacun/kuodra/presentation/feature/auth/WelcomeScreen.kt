package com.arenacun.kuodra.presentation.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.R
import com.arenacun.kuodra.presentation.app.StartState
import com.arenacun.kuodra.presentation.component.KIcon
import com.arenacun.kuodra.presentation.component.KLogoMark
import com.arenacun.kuodra.presentation.theme.Kuodra
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val taglinePhrases = listOf(
    "entre roomies", "en tu negocio", "con tu pareja",
    "con la familia", "en tu changarro", "en el viaje", "contigo mismo",
)

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onVerified: (StartState) -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
    redirectBus: OAuthRedirectBus = koinInject(),
) {
    val c = Kuodra.colors
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Abre la pantalla de consentimiento de Google en el navegador del sistema (Custom Tab).
    LaunchedEffect(Unit) {
        viewModel.openOAuthUrl.collect { url ->
            CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
        }
    }
    // El App Link de redirect entra por MainActivity → el bus; lo consumimos y lo canjeamos.
    LaunchedEffect(Unit) {
        redirectBus.callbacks.collect { cb ->
            redirectBus.clear()
            viewModel.completeOAuth2(cb.code, cb.state)
        }
    }
    // Login con Google completado → mismo enrutado que el OTP (nombre/onboarding/dashboard).
    LaunchedEffect(Unit) {
        viewModel.oauthVerified.collect { onVerified(it) }
    }

    var phraseIdx by remember { mutableIntStateOf(0) }
    var charCount by remember { mutableIntStateOf(0) }
    var deleting by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val phrase = " " + taglinePhrases[phraseIdx]
            if (!deleting) {
                if (charCount < phrase.length) { charCount++; delay(60) }
                else { delay(1300); deleting = true }
            } else {
                if (charCount > 0) { charCount--; delay(35) }
                else { deleting = false; phraseIdx = (phraseIdx + 1) % taglinePhrases.size }
            }
        }
    }
    val suffix = (" " + taglinePhrases[phraseIdx]).take(charCount)

    Column(
        Modifier
            .fillMaxSize()
            .background(c.screenBg)
            .padding(horizontal = 26.dp, vertical = 34.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            KLogoMark(boxSize = 76.dp, cornerRadius = 22.dp, background = c.primary, foreground = c.primaryInk)
            Text("Kuodra", style = Kuodra.type.displayAmount, color = c.ink,
                modifier = Modifier.padding(top = 22.dp))
            Text("by Arenacun", style = Kuodra.type.caption, color = c.ink3,
                modifier = Modifier.padding(top = 7.dp))
            Row(
                Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Que las cuentas cuadren", style = Kuodra.type.body, color = c.ink2)
                Text(suffix, style = Kuodra.type.body, color = c.primary)
                Text("|", style = Kuodra.type.body, color = c.primary)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Continuar con Google (outline) — OAuth2 vía Custom Tabs + redirect por App Link.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(Kuodra.shape.lg)
                    .background(c.surface)
                    .border(BorderStroke(1.5.dp, c.line), Kuodra.shape.lg)
                    .clickable(enabled = !state.googleLoading) { viewModel.startGoogleSignIn() }
                    .padding(15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KIcon(R.drawable.ic_google, size = 20.dp, tint = c.ink2)
                Spacer(Modifier.width(11.dp))
                Text(
                    if (state.googleLoading) "Conectando…" else "Continuar con Google",
                    style = Kuodra.type.heading, color = c.ink2,
                )
            }
            state.authError?.let { error ->
                Text(
                    error, style = Kuodra.type.caption, color = c.neg,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Continuar con correo (primary)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(Kuodra.shape.lg)
                    .background(c.primary)
                    .clickable { onContinue() }
                    .padding(15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KIcon(R.drawable.ic_mail, size = 20.dp, tint = c.primaryInk)
                Spacer(Modifier.width(11.dp))
                Text("Continuar con correo", style = Kuodra.type.heading, color = c.primaryInk)
            }
            Text(
                "Al continuar aceptas los Términos y el Aviso de privacidad.",
                style = Kuodra.type.caption, color = c.ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}
