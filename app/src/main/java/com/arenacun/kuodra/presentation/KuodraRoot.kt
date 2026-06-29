package com.arenacun.kuodra.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.presentation.app.AppViewModel
import com.arenacun.kuodra.presentation.app.StartState
import com.arenacun.kuodra.presentation.component.KLogoMark
import com.arenacun.kuodra.presentation.navigation.Destination
import com.arenacun.kuodra.presentation.navigation.KuodraNavHost
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraTheme
import org.koin.androidx.compose.koinViewModel

/** Punto de entrada Compose: aplica el tema observado y monta el grafo de navegación. */
@Composable
fun KuodraRoot(appViewModel: AppViewModel = koinViewModel()) {
    val dark by appViewModel.darkTheme.collectAsStateWithLifecycle()
    val start by appViewModel.start.collectAsStateWithLifecycle()
    KuodraTheme(darkTheme = dark) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Kuodra.colors.screenBg)
                .systemBarsPadding(),
        ) {
            if (start == StartState.Loading) {
                Splash()
            } else {
                KuodraNavHost(startDestination = start.toDestination())
            }
        }
    }
}

/** Pantalla mínima mientras se restaura la sesión (evita el parpadeo de Welcome). */
@Composable
private fun Splash() {
    val c = Kuodra.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        KLogoMark(boxSize = 76.dp, cornerRadius = 22.dp, background = c.primary, foreground = c.primaryInk)
    }
}

private fun StartState.toDestination(): Destination = when (this) {
    StartState.NeedsName -> Destination.Name
    StartState.Onboarding -> Destination.Mode
    StartState.Ready -> Destination.Dashboard
    // Loading no llega aquí; LoggedOut y el fallback arrancan en el flujo de auth.
    StartState.Loading, StartState.LoggedOut -> Destination.AuthGraph
}
