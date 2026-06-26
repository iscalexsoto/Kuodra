package com.arenacun.kuodra.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalKuodraColors = staticCompositionLocalOf { KuodraLight }

@Composable
fun KuodraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) KuodraDark else KuodraLight
    CompositionLocalProvider(
        LocalKuodraColors provides colors,
        content = content,
    )
}

/** Accesor corto: Kuodra.colors.primary, Kuodra.type.titleScreen, Kuodra.shape.xl … */
object Kuodra {
    val colors: KuodraColors
        @Composable @ReadOnlyComposable get() = LocalKuodraColors.current

    val type get() = KuodraType
    val shape get() = KuodraShapes
    val dims get() = Dimens
}
