package com.arenacun.kuodra.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.presentation.app.AppViewModel
import com.arenacun.kuodra.presentation.navigation.KuodraNavHost
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraTheme
import org.koin.androidx.compose.koinViewModel

/** Punto de entrada Compose: aplica el tema observado y monta el grafo de navegación. */
@Composable
fun KuodraRoot(appViewModel: AppViewModel = koinViewModel()) {
    val dark by appViewModel.darkTheme.collectAsStateWithLifecycle()
    KuodraTheme(darkTheme = dark) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Kuodra.colors.screenBg)
                .systemBarsPadding(),
        ) {
            KuodraNavHost()
        }
    }
}
