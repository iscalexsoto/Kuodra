package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import com.arenacun.kuodra.presentation.theme.Kuodra

/**
 * Hoja inferior de Kuodra: `ModalBottomSheet` de Material3 con los tokens del tema (el tema
 * usa `KuodraColors`, no `ColorScheme`, así que los colores se pasan explícitos). Centraliza
 * el override para no repetirlo en cada sheet (categoría, pagador, dividir, filtros, menú).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuodraBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Kuodra.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        contentColor = c.ink,
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.line) },
        content = content,
    )
}
