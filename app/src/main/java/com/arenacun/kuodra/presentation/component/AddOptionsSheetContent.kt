package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arenacun.kuodra.R
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors

/**
 * Contenido del sheet "Agregar": las 3 vías de alta de un movimiento (escanear ticket con la
 * cámara, elegir foto de la galería o capturar a mano). Stateless y reusable: el estado del sheet
 * vive en el ViewModel host (hoy el Dashboard, patrón `DashboardSheet`).
 */
@Composable
fun AddOptionsSheetContent(
    c: KuodraColors,
    onScan: () -> Unit,
    onGallery: () -> Unit,
    onManual: () -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Agregar", style = Kuodra.type.titleScreen, color = c.ink)
            Box(
                Modifier.size(30.dp).clip(Kuodra.shape.pill).background(c.surface2)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { KIcon(R.drawable.ic_close, 12.dp, c.ink3) }
        }
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AddOptionRow(
                c = c,
                iconRes = R.drawable.ic_camera,
                iconBg = c.tint,
                iconTint = c.tintInk,
                title = "Escanear ticket",
                sub = "Toma una foto y llenamos el movimiento",
                onClick = onScan,
            )
            AddOptionRow(
                c = c,
                iconRes = R.drawable.ic_image_up,
                iconBg = c.posTint,
                iconTint = c.pos,
                title = "Tomar de la galería",
                sub = "Elige la foto de un ticket que ya tengas",
                onClick = onGallery,
            )
            AddOptionRow(
                c = c,
                iconRes = R.drawable.ic_notebook,
                iconBg = c.warnTint,
                iconTint = c.warn,
                title = "Capturar manualmente",
                sub = "Escribe el movimiento tú mismo",
                onClick = onManual,
            )
        }
    }
}

@Composable
private fun AddOptionRow(
    c: KuodraColors,
    iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    title: String,
    sub: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface2)
            .clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(Kuodra.shape.md).background(iconBg),
            contentAlignment = Alignment.Center,
        ) { KIcon(iconRes, 20.dp, iconTint) }
        Column(Modifier.weight(1f)) {
            Text(title, style = Kuodra.type.heading, color = c.ink)
            Text(sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
        }
    }
}
