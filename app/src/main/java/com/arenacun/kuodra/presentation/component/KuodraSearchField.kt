package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.arenacun.kuodra.presentation.theme.Kuodra

/**
 * Campo de búsqueda reutilizable: fila redondeada (surface2 + borde line) con [SearchGlyph]
 * y un [BasicTextField] con placeholder. Stateless: el dueño del estado pasa `value`/`onValueChange`.
 */
@Composable
fun KuodraSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = Kuodra.colors
    Row(
        modifier.clip(Kuodra.shape.lg).background(c.surface2)
            .border(1.dp, c.line, Kuodra.shape.lg).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchGlyph(16.dp, c.ink3)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Kuodra.type.body.copy(color = c.ink),
            cursorBrush = SolidColor(c.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, style = Kuodra.type.body, color = c.ink3)
                inner()
            },
        )
    }
}
