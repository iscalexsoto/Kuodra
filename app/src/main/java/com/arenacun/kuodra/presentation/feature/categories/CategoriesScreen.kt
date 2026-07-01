package com.arenacun.kuodra.presentation.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.CategoryEditSheet
import com.arenacun.kuodra.presentation.component.CategoryTag
import com.arenacun.kuodra.presentation.component.Chevron
import com.arenacun.kuodra.presentation.component.KuodraBottomSheet
import com.arenacun.kuodra.presentation.component.KuodraSearchField
import com.arenacun.kuodra.presentation.component.PlusIcon
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla dedicada a gestionar el catálogo de categorías, con buscador. La estática
 * "Sin categoría" se lista pero no se edita. Crear/editar/borrar usan [CategoryEditSheet].
 */
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(c.screenBg).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 2.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = onBack)
            Text("Categorías", style = Kuodra.type.heading, color = c.ink)
        }

        KuodraSearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Buscar categoría…",
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )

        Card(c) {
            Column(Modifier.fillMaxWidth()) {
                if (state.categories.isEmpty()) {
                    Text(
                        "Sin resultados", style = Kuodra.type.body, color = c.ink3,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 16.dp),
                    )
                }
                state.categories.forEach { cat ->
                    val editable = !cat.isStatic
                    Row(
                        Modifier.fillMaxWidth()
                            .then(if (editable) Modifier.clickable { viewModel.onEditCategory(cat) } else Modifier)
                            .padding(horizontal = 15.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryTag(cat.tag, cat.tone, size = 36.dp)
                        Text(cat.name, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
                        if (editable) Chevron(7.dp, c.ink3, degrees = 0f)
                    }
                    Divider(c)
                }
                Row(
                    Modifier.fillMaxWidth().clickable { viewModel.onStartCreateCategory() }
                        .padding(horizontal = 15.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).clip(Kuodra.shape.pill).background(c.tint),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlusIcon(16.dp, c.tintInk)
                    }
                    Text("Crear categoría", style = Kuodra.type.body, color = c.tintInk)
                }
            }
        }
    }

    state.editingCategory?.let { draft ->
        KuodraBottomSheet(onDismiss = viewModel::onCloseCategory) {
            CategoryEditSheet(
                c = c,
                draft = draft,
                onName = viewModel::onCategoryDraftName,
                onTone = viewModel::onCategoryDraftTone,
                onConfirm = viewModel::onConfirmCategory,
                onDelete = if (draft.original != null) viewModel::onDeleteCategory else null,
            )
        }
    }
}

@Composable
private fun Card(c: KuodraColors, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface).border(1.dp, c.line, Kuodra.shape.xl)) {
        content()
    }
}

@Composable
private fun Divider(c: KuodraColors) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
}
