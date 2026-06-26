package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arenacun.kuodra.presentation.theme.Kuodra

/**
 * Card base: surface + borde 1px line + 18r + sombra card.
 * Contenedor de listas (people/categories/movements) y bloques de contenido.
 * Para sombra real usa Modifier.shadow(1.dp, shape) antes de clip.
 */
@Composable
fun KuodraCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Kuodra.colors
    val shape = Kuodra.shape.xl // 18
    Column(
        modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape),
        content = content,
    )
}
