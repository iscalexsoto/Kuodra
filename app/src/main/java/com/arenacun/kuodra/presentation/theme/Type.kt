package com.arenacun.kuodra.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arenacun.kuodra.R

// Coloca los .ttf en res/font/ con estos nombres (o ajusta los identificadores).
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
)

val SpaceMono = FontFamily(
    Font(R.font.space_mono_bold, FontWeight.Bold),
)

object KuodraType {
    // Space Grotesk — títulos y montos
    val displayAmount = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, letterSpacing = (-2).sp,
    )
    val titleScreen = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, letterSpacing = (-0.6).sp,
    )
    val heading = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
    )

    // Plus Jakarta Sans — cuerpo y UI
    val body = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 22.sp,
    )
    val caption = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
    )

    // Space Mono — etiquetas / overline
    val overline = TextStyle(
        fontFamily = SpaceMono, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, letterSpacing = 0.8.sp,
    )
}
