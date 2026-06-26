package com.arenacun.kuodra.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Todos los tokens de color de Kuodra. Un campo por token.
 * Una instancia por tema (claro/oscuro), mismos nombres — distintos valores.
 * Equivalente a las variables CSS del prototipo (--primary, --surface, …).
 */
@Immutable
data class KuodraColors(
    val screenBg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val primary: Color,
    val primaryInk: Color,
    val tint: Color,
    val tintInk: Color,
    val pos: Color,
    val posTint: Color,
    val neg: Color,
    val negTint: Color,
    val warn: Color,
    val warnTint: Color,
    val shadow: Color,
    val isDark: Boolean,
)

val KuodraLight = KuodraColors(
    screenBg = Color(0xFFF1F1F6),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF6F6FA),
    ink = Color(0xFF1A1A22),
    ink2 = Color(0xFF4A4A56),
    ink3 = Color(0xFF8A8A95),
    line = Color(0xFFEBEBF1),
    primary = Color(0xFF5B4FE0),
    primaryInk = Color(0xFFFFFFFF),
    tint = Color(0xFFECEAFB),
    tintInk = Color(0xFF4A3FC2),
    pos = Color(0xFF00875A),
    posTint = Color(0xFFE4F3EC),
    neg = Color(0xFFDE3C4B),
    negTint = Color(0xFFFBE9EB),
    warn = Color(0xFFE8920C),
    warnTint = Color(0xFFFBF0DE),
    shadow = Color(0x0F141428),
    isDark = false,
)

val KuodraDark = KuodraColors(
    screenBg = Color(0xFF0E0E13),
    surface = Color(0xFF1A1A22),
    surface2 = Color(0xFF22222C),
    ink = Color(0xFFF2F2F6),
    ink2 = Color(0xFFB4B4BE),
    ink3 = Color(0xFF7E7E89),
    line = Color(0xFF2A2A34),
    primary = Color(0xFF8278EC),
    primaryInk = Color(0xFF0C0A1E),
    tint = Color(0xFF262240),
    tintInk = Color(0xFFB9B1F6),
    pos = Color(0xFF2DB877),
    posTint = Color(0xFF15271E),
    neg = Color(0xFFF0667A),
    negTint = Color(0xFF2C1A1E),
    warn = Color(0xFFE8A33C),
    warnTint = Color(0xFF2A2114),
    shadow = Color(0x66000000),
    isDark = true,
)
