package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.arenacun.kuodra.presentation.theme.Kuodra

enum class KuodraButtonVariant { Primary, Outline, Danger }

/**
 * Botón base de Kuodra. Tres variantes (ver inventario del handoff).
 * Lee solo tokens — sin colores ni medidas literales.
 */
@Composable
fun KuodraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KuodraButtonVariant = KuodraButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val colors = Kuodra.colors

    val shape = when (variant) {
        KuodraButtonVariant.Primary -> Kuodra.shape.lg   // 16
        else -> Kuodra.shape.lg.copy()                    // 14 visual; usa md si prefieres
    }
    val pad = if (variant == KuodraButtonVariant.Primary) 17.dp else 13.dp

    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .let { m ->
            when (variant) {
                KuodraButtonVariant.Primary -> m.background(if (enabled) colors.primary else colors.line)
                KuodraButtonVariant.Danger -> m.background(colors.neg)
                KuodraButtonVariant.Outline -> m
                    .background(colors.surface)
                    .border(BorderStroke(1.5.dp, colors.line), shape)
            }
        }
        .clickable(enabled = enabled, onClick = onClick)
        .padding(vertical = pad)

    val textColor = when (variant) {
        KuodraButtonVariant.Primary -> colors.primaryInk
        KuodraButtonVariant.Danger -> colors.primaryInk
        KuodraButtonVariant.Outline -> colors.ink
    }

    Box(base, contentAlignment = Alignment.Center) {
        Text(
            text,
            style = Kuodra.type.heading,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
