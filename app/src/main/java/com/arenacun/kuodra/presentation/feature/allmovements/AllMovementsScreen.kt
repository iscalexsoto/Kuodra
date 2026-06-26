package com.arenacun.kuodra.presentation.feature.allmovements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.domain.usecase.MovementGroup
import com.arenacun.kuodra.domain.usecase.MovementPeriod
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.CategoryTag
import com.arenacun.kuodra.presentation.component.Chevron
import com.arenacun.kuodra.presentation.component.KuodraBottomSheet
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun AllMovementsScreen(
    onBack: () -> Unit,
    onOpenMovement: (String) -> Unit,
    viewModel: AllMovementsViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(c.screenBg)) {
        Column(Modifier.fillMaxSize()) {
            // top bar
            Row(
                Modifier.fillMaxWidth().background(c.surface)
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BackCircle(onClick = onBack)
                Text("Todos los movimientos", style = Kuodra.type.heading, color = c.ink,
                    modifier = Modifier.weight(1f))
                IconButtonCircle(c, onClick = viewModel::onOpenSearch) { SearchGlyph(16.dp, c.ink2) }
                IconButtonCircle(c, onClick = viewModel::onOpenFilter) { FilterGlyph(16.dp, c.ink2) }
            }

            // quick filter chips
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickChip(c, "Todo", state.filter.categories.isEmpty()) { viewModel.onClearFilters() }
                state.allCategories.forEach { cat ->
                    QuickChip(c, cat, cat in state.filter.categories) { viewModel.onToggleCategory(cat) }
                }
            }

            // grouped list
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 24.dp),
            ) {
                if (state.groups.isEmpty()) {
                    Text("Sin movimientos", style = Kuodra.type.body, color = c.ink3,
                        modifier = Modifier.padding(top = 24.dp))
                }
                state.groups.forEach { group ->
                    GroupSection(c, group, onOpenMovement)
                }
            }
        }

        if (state.showSearch) {
            SearchOverlay(
                c = c,
                query = state.filter.query,
                groups = state.groups,
                onQuery = viewModel::onQueryChange,
                onClose = viewModel::onCloseSearch,
                onOpenMovement = onOpenMovement,
            )
        }
        if (state.showFilter) {
            KuodraBottomSheet(onDismiss = viewModel::onCloseFilter) {
                FilterSheet(c, state, viewModel)
            }
        }
    }
}

@Composable
private fun GroupSection(c: KuodraColors, group: MovementGroup, onOpenMovement: (String) -> Unit) {
    Text(group.header.uppercase(), style = Kuodra.type.overline, color = c.ink3,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp))
    Column(
        Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface)
            .border(1.dp, c.line, Kuodra.shape.xl),
    ) {
        group.movements.forEachIndexed { i, m ->
            MovementRow(c, m) { onOpenMovement(m.id) }
            if (i < group.movements.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        }
    }
}

@Composable
private fun MovementRow(c: KuodraColors, m: Movement, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTag(m.catTag, m.tone)
        Column(Modifier.weight(1f)) {
            Text(m.title, style = Kuodra.type.body, color = c.ink)
            Text(m.meta, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
        }
        Text(m.amount, style = Kuodra.type.heading, color = c.ink)
    }
}

@Composable
private fun QuickChip(c: KuodraColors, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(Kuodra.shape.pill)
            .background(if (selected) c.primary else c.surface)
            .border(1.dp, if (selected) c.primary else c.line, Kuodra.shape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = Kuodra.type.caption, color = if (selected) c.primaryInk else c.ink2)
    }
}

@Composable
private fun SearchOverlay(
    c: KuodraColors,
    query: String,
    groups: List<MovementGroup>,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    onOpenMovement: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(c.screenBg)) {
        Row(
            Modifier.fillMaxWidth().background(c.surface)
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = onClose)
            Row(
                Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.surface2)
                    .border(1.dp, c.line, Kuodra.shape.lg).padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SearchGlyph(16.dp, c.ink3)
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = Kuodra.type.body.copy(color = c.ink),
                    cursorBrush = SolidColor(c.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Buscar movimiento…", style = Kuodra.type.body, color = c.ink3)
                        inner()
                    },
                )
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 24.dp),
        ) {
            if (groups.isEmpty()) {
                Text("Sin resultados", style = Kuodra.type.body, color = c.ink3,
                    modifier = Modifier.padding(top = 24.dp))
            }
            groups.forEach { GroupSection(c, it, onOpenMovement) }
        }
    }
}

@Composable
private fun FilterSheet(
    c: KuodraColors,
    state: AllMovementsUiState,
    viewModel: AllMovementsViewModel,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text("Filtrar", style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(bottom = 4.dp))

        FilterLabel(c, "CATEGORÍA")
        WrapChips(state.allCategories) { cat ->
            ToggleChip(c, cat, cat in state.filter.categories) { viewModel.onToggleCategory(cat) }
        }

        FilterLabel(c, "PERIODO")
        WrapChips(PERIODS) { (period, label) ->
            ToggleChip(c, label, state.filter.period == period) { viewModel.onSetPeriod(period) }
        }

        if (state.allResponsibles.isNotEmpty()) {
            FilterLabel(c, "RESPONSABLE")
            WrapChips(state.allResponsibles) { name ->
                ToggleChip(c, name, name in state.filter.responsibles) { viewModel.onToggleResponsible(name) }
            }
        }

        Box(
            Modifier.fillMaxWidth().padding(top = 18.dp).clip(Kuodra.shape.lg).background(c.primary)
                .clickable(onClick = viewModel::onCloseFilter).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Mostrar ${state.shownCount} movimientos", style = Kuodra.type.heading, color = c.primaryInk) }
    }
}

@Composable
private fun FilterLabel(c: KuodraColors, text: String) {
    Text(text, style = Kuodra.type.overline, color = c.ink3,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
private fun ToggleChip(c: KuodraColors, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(Kuodra.shape.pill)
            .background(if (selected) c.tint else c.surface2)
            .border(1.dp, if (selected) c.primary else c.line, Kuodra.shape.pill)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(label, style = Kuodra.type.caption, color = if (selected) c.tintInk else c.ink2)
    }
}

// ---- iconos ----

@Composable
private fun IconButtonCircle(c: KuodraColors, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(Kuodra.shape.pill).background(c.surface2)
            .border(1.dp, c.line, Kuodra.shape.pill).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> WrapChips(items: List<T>, item: @Composable (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item(it) }
    }
}

@Composable
private fun SearchGlyph(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension * 0.32f
        val cx = this.size.width * 0.42f
        val cy = this.size.height * 0.42f
        drawCircle(color, radius = r, center = Offset(cx, cy), style = Stroke(width = size.toPx() * 0.12f))
        drawLine(color, Offset(cx + r * 0.8f, cy + r * 0.8f),
            Offset(this.size.width * 0.86f, this.size.height * 0.86f), strokeWidth = size.toPx() * 0.12f)
    }
}

@Composable
private fun FilterGlyph(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val t = size.toPx() * 0.12f
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.2f); lineTo(w * 0.9f, h * 0.2f)
            lineTo(w * 0.58f, h * 0.55f); lineTo(w * 0.58f, h * 0.85f)
            lineTo(w * 0.42f, h * 0.72f); lineTo(w * 0.42f, h * 0.55f); close()
        }
        drawPath(path, color, style = Stroke(width = t))
    }
}

private val PERIODS = listOf(
    MovementPeriod.ThisWeek to "Esta semana",
    MovementPeriod.ThisMonth to "Este mes",
    MovementPeriod.LastMonth to "Mes anterior",
)
