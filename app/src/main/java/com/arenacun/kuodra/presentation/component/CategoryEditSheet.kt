package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors

/** Borrador de creación/edición de categoría (compartido por Agregar movimiento y Ajustes). */
data class CategoryDraft(
    /** Categoría original si se está editando; null si es nueva. */
    val original: Category? = null,
    val name: String = "",
    val tone: AvatarTone = AvatarTone.Tint,
)

/**
 * Hoja para crear o renombrar una categoría: nombre + selector de color (tono). La etiqueta corta
 * se deriva del nombre. Stateless: el ViewModel posee el [CategoryDraft].
 */
@Composable
fun CategoryEditSheet(
    c: KuodraColors,
    draft: CategoryDraft,
    onName: (String) -> Unit,
    onTone: (AvatarTone) -> Unit,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val tag = if (draft.name.isBlank()) "Aa" else Category.deriveTag(draft.name)
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text(
            if (draft.original == null) "Crear categoría" else "Editar categoría",
            style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 12.dp),
        )

        Text("NOMBRE", style = Kuodra.type.overline, color = c.ink3, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface2)
                .border(1.dp, c.line, Kuodra.shape.lg).padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            BasicTextField(
                value = draft.name,
                onValueChange = onName,
                singleLine = true,
                textStyle = Kuodra.type.body.copy(color = c.ink),
                cursorBrush = SolidColor(c.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (draft.name.isEmpty()) Text("Nombre de la categoría", style = Kuodra.type.body, color = c.ink3)
                    inner()
                },
            )
        }

        Text("COLOR", style = Kuodra.type.overline, color = c.ink3,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AvatarTone.entries.forEach { tone ->
                val selected = tone == draft.tone
                Box(
                    Modifier.clip(Kuodra.shape.md)
                        .border(2.dp, if (selected) c.primary else c.line, Kuodra.shape.md)
                        .clickable { onTone(tone) }.padding(4.dp),
                ) { CategoryTag(tag, tone, size = 38.dp) }
            }
        }

        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onDelete != null) {
                Box(
                    Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.negTint)
                        .clickable(onClick = onDelete).padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Eliminar", style = Kuodra.type.heading, color = c.neg) }
            }
            val canSave = draft.name.isNotBlank()
            Box(
                Modifier.weight(1f).clip(Kuodra.shape.lg)
                    .background(if (canSave) c.primary else c.surface2)
                    .clickable(enabled = canSave, onClick = onConfirm).padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Guardar", style = Kuodra.type.heading, color = if (canSave) c.primaryInk else c.ink3) }
        }
    }
}
