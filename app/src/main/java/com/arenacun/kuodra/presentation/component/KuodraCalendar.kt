package com.arenacun.kuodra.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arenacun.kuodra.domain.model.CalendarCell
import com.arenacun.kuodra.domain.model.CalendarMonth
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import java.time.LocalDate

/**
 * Calendario del prototipo (`reference/Calendar.dc.html`): rejilla mensual con domingo
 * primero, fechas futuras deshabilitadas, hoy con anillo y seleccionado en `primary`. El mes
 * visible es estado **transitorio de UI** (`remember`); la fecha elegida sube por [onPick].
 */
@Composable
fun KuodraCalendar(
    selected: LocalDate?,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Kuodra.colors
    var visible by remember(selected, today) {
        mutableStateOf(CalendarMonth.forSelection(selected, today))
    }

    Column(modifier.fillMaxWidth().clip(Kuodra.shape.xxl).background(c.surface).padding(14.dp)) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NavCircle(c, degrees = 180f, enabled = true) {
                visible = CalendarMonth.prev(visible, today)
            }
            Text(visible.title, style = Kuodra.type.heading, color = c.ink)
            NavCircle(c, degrees = 0f, enabled = visible.canGoNext) {
                visible = CalendarMonth.next(visible, today)
            }
        }

        // weekday labels
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            CalendarMonth.WEEKDAYS.forEach { d ->
                Text(d, style = Kuodra.type.overline, color = c.ink3, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f))
            }
        }

        // day grid (rows of 7)
        Column(Modifier.padding(top = 4.dp)) {
            visible.cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { cell -> DayCell(c, cell, selected, today, onPick) }
                    // completa la última semana para mantener el ancho de columna
                    repeat(7 - week.size) { Box(Modifier.weight(1f).height(42.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    c: KuodraColors,
    cell: CalendarCell,
    selected: LocalDate?,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
) {
    Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
        val date = cell.date ?: return@Box
        val isSelected = date == selected
        val isToday = date == today
        val bg = if (isSelected) c.primary else Color.Transparent
        val ink = when {
            isSelected -> c.primaryInk
            !cell.enabled -> c.ink3
            else -> c.ink
        }
        val ring = if (isToday && !isSelected) Modifier.border(2.dp, c.primary, Kuodra.shape.pill) else Modifier
        Box(
            Modifier.size(36.dp).clip(Kuodra.shape.pill).background(bg).then(ring)
                .clickable(enabled = cell.enabled) { onPick(date) },
            contentAlignment = Alignment.Center,
        ) { Text(date.dayOfMonth.toString(), style = Kuodra.type.body, color = ink) }
    }
}

@Composable
private fun NavCircle(c: KuodraColors, degrees: Float, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(Kuodra.shape.pill).background(c.surface2)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Chevron(12.dp, if (enabled) c.ink2 else c.ink3, degrees = degrees) }
}
