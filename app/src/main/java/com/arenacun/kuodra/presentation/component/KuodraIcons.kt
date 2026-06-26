package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import com.arenacun.kuodra.domain.model.AvatarTone

/** Par (fondo, tinta) de una paleta de avatar, derivado de los tokens tint/pos/warn/neg. */
data class AvatarPalette(val bg: Color, val ink: Color)

/** Mapea un tono de avatar a su par de tokens. */
fun KuodraColors.avatar(tone: AvatarTone): AvatarPalette = when (tone) {
    AvatarTone.Tint -> AvatarPalette(tint, tintInk)
    AvatarTone.Pos -> AvatarPalette(posTint, pos)
    AvatarTone.Warn -> AvatarPalette(warnTint, warn)
    AvatarTone.Neg -> AvatarPalette(negTint, neg)
}

fun KuodraColors.avatarBg(tone: AvatarTone): Color = avatar(tone).bg
fun KuodraColors.avatarInk(tone: AvatarTone): Color = avatar(tone).ink

/**
 * Logo "K" de Kuodra: rejilla 2×2 donde la diagonal está cortada para formar la K.
 * `box` es el lado del contenedor cuadrado; `cell` el lado de la rejilla interior.
 */
@Composable
fun KLogoMark(
    boxSize: Dp,
    cornerRadius: Dp,
    background: Color,
    foreground: Color,
    gridSize: Dp = boxSize * 0.52f,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(boxSize)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(gridSize)) {
            val gap = size.width * 0.12f
            val cell = (size.width - gap) / 2f
            val r = cell * 0.16f
            // top-left full
            drawRoundRect(foreground, Offset(0f, 0f), androidx.compose.ui.geometry.Size(cell, cell),
                androidx.compose.ui.geometry.CornerRadius(r, r))
            // bottom-left full
            drawRoundRect(foreground, Offset(0f, cell + gap), androidx.compose.ui.geometry.Size(cell, cell),
                androidx.compose.ui.geometry.CornerRadius(r, r))
            // top-right: lower-left triangle (clip-path polygon(0 0,100% 0,0 100%))
            val tr = Path().apply {
                moveTo(cell + gap, 0f); lineTo(size.width, 0f); lineTo(cell + gap, cell); close()
            }
            drawPath(tr, foreground)
            // bottom-right: upper-left triangle (clip-path polygon(0 0,0 100%,100% 100%))
            val br = Path().apply {
                moveTo(cell + gap, cell + gap); lineTo(cell + gap, size.height); lineTo(size.width, size.height); close()
            }
            drawPath(br, foreground)
        }
    }
}

/** Cruz "+" dibujada con dos barras redondeadas. */
@Composable
fun PlusIcon(size: Dp, color: Color, thickness: Dp = 2.5.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val t = thickness.toPx()
        val r = androidx.compose.ui.geometry.CornerRadius(t / 2, t / 2)
        drawRoundRect(color, Offset(0f, (this.size.height - t) / 2),
            androidx.compose.ui.geometry.Size(this.size.width, t), r)
        drawRoundRect(color, Offset((this.size.width - t) / 2, 0f),
            androidx.compose.ui.geometry.Size(t, this.size.height), r)
    }
}

/** Galón (chevron). direction en grados: 0 = ">", 90 = "v", 180 = "<", 270 = "^". */
@Composable
fun Chevron(size: Dp, color: Color, degrees: Float = 0f, thickness: Dp = 2.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        rotate(degrees) {
            val w = this.size.width
            val h = this.size.height
            val stroke = Stroke(width = thickness.toPx())
            val path = Path().apply {
                moveTo(w * 0.35f, h * 0.2f)
                lineTo(w * 0.65f, h * 0.5f)
                lineTo(w * 0.35f, h * 0.8f)
            }
            drawPath(path, color, style = stroke)
        }
    }
}

/** Avatar con paleta tonal (bg/ink) — variante de Avatar para personas con color propio. */
@Composable
fun ToneAvatar(
    initials: String,
    tone: AvatarTone,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val pal = Kuodra.colors.avatar(tone)
    Box(
        modifier
            .size(size)
            .clip(Kuodra.shape.pill)
            .background(pal.bg),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            initials,
            style = Kuodra.type.heading,
            color = pal.ink,
        )
    }
}

/** Caja de categoría (cuadrada con esquinas md) con etiqueta corta. */
@Composable
fun CategoryTag(
    tag: String,
    tone: AvatarTone,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val pal = Kuodra.colors.avatar(tone)
    Box(
        modifier
            .size(size)
            .clip(Kuodra.shape.md)
            .background(pal.bg),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(tag, style = Kuodra.type.caption, color = pal.ink)
    }
}
