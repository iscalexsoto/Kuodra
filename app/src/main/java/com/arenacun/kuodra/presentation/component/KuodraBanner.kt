package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.arenacun.kuodra.presentation.theme.Kuodra

enum class BannerTone { Warn, Positive, Negative }

/**
 * Banner semántico: fondo *Tint, 16r, ícono en surface 11r.
 * Úsalo para recordatorios de corte, fondo bajo, pendientes, etc.
 */
@Composable
fun KuodraBanner(
    title: String,
    subtitle: String,
    tone: BannerTone = BannerTone.Warn,
    leadingIcon: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Kuodra.colors
    val (accent: Color, tintBg: Color) = when (tone) {
        BannerTone.Warn -> colors.warn to colors.warnTint
        BannerTone.Positive -> colors.pos to colors.posTint
        BannerTone.Negative -> colors.neg to colors.negTint
    }
    val shape = Kuodra.shape.lg // 16

    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tintBg)
            .border(1.dp, accent.copy(alpha = 0.25f), shape)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(Kuodra.shape.md) // ~11–12
                .background(colors.surface),
            contentAlignment = Alignment.Center,
            content = { leadingIcon() },
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = Kuodra.type.caption, color = colors.ink)
            Text(subtitle, style = Kuodra.type.caption, color = colors.ink3)
        }
    }
}
