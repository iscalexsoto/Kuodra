package com.arenacun.kuodra.presentation.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.presentation.feature.movement.MovementUi
import com.arenacun.kuodra.presentation.component.CategoryTag
import com.arenacun.kuodra.presentation.component.Chevron
import com.arenacun.kuodra.presentation.component.KuodraBottomSheet
import com.arenacun.kuodra.presentation.component.KLogoMark
import com.arenacun.kuodra.presentation.component.PlusIcon
import com.arenacun.kuodra.presentation.component.ToneAvatar
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(
    onAddMovement: () -> Unit,
    onOpenMovement: (String) -> Unit,
    onSeeAllMovements: () -> Unit,
    onOpenSettings: () -> Unit,
    onSettle: () -> Unit,
    onReplenish: () -> Unit,
    onOpenHistory: () -> Unit,
    onCreateSpace: (UseCase) -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()
    val t = state.space.terminology
    val uc = state.useCase

    Box(Modifier.fillMaxSize().background(c.screenBg)) {
        Column(Modifier.fillMaxSize()) {
            // ===== Top app bar =====
            Row(
                Modifier.fillMaxWidth().background(c.surface)
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // tocar el título abre el selector de espacios "Tus espacios"
                Row(
                    Modifier.weight(1f).clickable { viewModel.onOpenSpaces() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KLogoMark(boxSize = 38.dp, cornerRadius = 11.dp, background = c.primary, foreground = c.primaryInk)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(state.space.displayName, style = Kuodra.type.heading, color = c.ink)
                            Chevron(8.dp, c.ink3, degrees = 90f)
                        }
                        Text("${t.containerKind} · ${t.roleLabel}",
                            style = Kuodra.type.caption, color = c.ink3,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }
                // botón de menú: abre el menú de espacios y acciones
                Box(
                    Modifier.size(36.dp).clip(Kuodra.shape.pill).background(c.surface2)
                        .border(1.dp, c.line, Kuodra.shape.pill)
                        .clickable { viewModel.onOpenMenu() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(3) {
                            Box(Modifier.size(4.dp).clip(Kuodra.shape.pill).background(c.ink2))
                        }
                    }
                }
            }

            // ===== Scroll content =====
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 96.dp),
            ) {
                when (uc) {
                    UseCase.Gastos -> ReminderBanner(c)
                    UseCase.Caja -> LowFundBanner(c, onReplenish)
                    UseCase.Personal -> {}
                }

                DashboardHero(c, uc, t.heroLabel, state.personalHero)

                if (uc != UseCase.Personal) {
                    Spacer(Modifier.height(12.dp))
                    SettleAction(c, uc, onSettle)
                }

                SectionHeader(
                    c,
                    title = when (uc) {
                        UseCase.Personal -> "Por categoría"
                        UseCase.Gastos -> "Quién debe a quién"
                        UseCase.Caja -> "Movimientos por persona"
                    },
                )
                if (uc == UseCase.Personal) {
                    CategoriesCard(c, state.categories)
                } else {
                    PeopleCard(c, state.people)
                }

                SectionHeader(
                    c, "Movimientos recientes",
                    onSeeAll = if (state.movements.isEmpty()) null else onSeeAllMovements,
                )
                MovementsCard(c, state.movements, onOpenMovement)
            }
        }

        // ===== FAB extendido =====
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 24.dp)
                .clip(Kuodra.shape.xl).background(c.primary)
                .clickable { onAddMovement() }
                .padding(start = 17.dp, end = 20.dp, top = 15.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PlusIcon(18.dp, c.primaryInk, thickness = 3.dp)
            Text("Agregar", style = Kuodra.type.heading, color = c.primaryInk)
        }

        // ===== Flujo salir/archivar (overlay con scrim) =====
        if (overlay.leaveStep != LeaveStep.None) {
            LeaveFlow(c, overlay.leaveStep, state.space.displayName,
                onSettle = onSettle, onAdvance = viewModel::onLeaveAdvance, onClose = viewModel::onLeaveClose)
        }
    }

    // ===== Hojas inferiores =====
    when (overlay.sheet) {
        DashboardSheet.Spaces -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            SpacesSheet(
                c = c,
                current = uc,
                onClose = viewModel::onCloseSheet,
                onSelectUseCase = viewModel::onSelectUseCase,
                onCreateSpace = viewModel::onOpenCreateSpace,
            )
        }
        DashboardSheet.CreateSpace -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            CreateSpaceSheet(
                c = c,
                onClose = viewModel::onCloseSheet,
                onPick = { useCase -> viewModel.onCloseSheet(); onCreateSpace(useCase) },
            )
        }
        DashboardSheet.Menu -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            SpaceMenu(
                c = c,
                current = uc,
                onClose = viewModel::onCloseSheet,
                onShare = viewModel::onShare,
                onOpenSettings = { viewModel.onCloseSheet(); onOpenSettings() },
                onClosePeriod = viewModel::onClosePeriod,
                onReplenish = { viewModel.onCloseSheet(); onReplenish() },
                onOpenHistory = { viewModel.onCloseSheet(); onOpenHistory() },
                onLeave = viewModel::onLeaveStart,
            )
        }
        DashboardSheet.Share -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            ShareSheet(c, uc, onClose = viewModel::onCloseSheet, onShare = viewModel::onShareConfirm)
        }
        DashboardSheet.Shared -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            ShareDoneSheet(c, uc, onClose = viewModel::onCloseSheet)
        }
        DashboardSheet.PCloseConfirm -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            ClosePeriodSheet(c, state.personalHero, onClose = viewModel::onCloseSheet, onConfirm = viewModel::onClosePeriodConfirm)
        }
        DashboardSheet.PClosed -> KuodraBottomSheet(onDismiss = viewModel::onCloseSheet) {
            ClosePeriodDoneSheet(c, onClose = viewModel::onCloseSheet)
        }
        DashboardSheet.None -> {}
    }
}

/**
 * Selector "Tus espacios": Personal primero, luego los demás espacios (grupos / caja chica).
 * Resalta el espacio actual y permite crear uno nuevo. Réplica del sheet `spaces` del prototipo.
 */
@Composable
private fun SpacesSheet(
    c: KuodraColors,
    current: UseCase,
    onClose: () -> Unit,
    onSelectUseCase: (UseCase) -> Unit,
    onCreateSpace: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SheetHeader(c, "Tus espacios", onClose)
        Column(Modifier.padding(horizontal = 16.dp)) {
            UseCase.entries.forEach { option ->
                val selected = option == current
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(Kuodra.shape.lg)
                        .background(if (selected) c.tint else c.surface)
                        .border(1.5.dp, if (selected) c.primary else c.line, Kuodra.shape.lg)
                        .clickable { onSelectUseCase(option) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryTag(spaceTag(option), spaceTone(option), size = 42.dp)
                    Column(Modifier.weight(1f)) {
                        Text(spaceTitle(option), style = Kuodra.type.heading, color = c.ink)
                        Text(spaceListSub(option), style = Kuodra.type.caption, color = c.ink3,
                            modifier = Modifier.padding(top = 1.dp))
                    }
                    if (selected) CheckCircle(c)
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(Kuodra.shape.lg)
                    .border(1.5.dp, c.line, Kuodra.shape.lg)
                    .clickable(onClick = onCreateSpace)
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).clip(Kuodra.shape.md).background(c.surface2),
                    contentAlignment = Alignment.Center) {
                    PlusIcon(17.dp, c.primary, thickness = 2.5.dp)
                }
                Text("Crear espacio", style = Kuodra.type.heading, color = c.primary)
            }
        }
    }
}

/**
 * Opciones de "Crear espacio": elige el tipo (grupo compartido o caja chica). Al elegir se
 * navega a la pantalla de nombrado, igual que en el onboarding.
 */
@Composable
private fun CreateSpaceSheet(
    c: KuodraColors,
    onClose: () -> Unit,
    onPick: (UseCase) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SheetHeader(c, "Crear espacio", onClose)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Tu espacio Personal ya existe. ¿Qué más quieres agregar?",
                style = Kuodra.type.caption, color = c.ink2,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            CreateSpaceOption(c, UseCase.Gastos, AvatarTone.Pos, "Grupo compartido",
                "Divide gastos con roomies, pareja o familia", onPick)
            Spacer(Modifier.height(9.dp))
            CreateSpaceOption(c, UseCase.Caja, AvatarTone.Warn, "Caja chica",
                "Maneja el fondo de efectivo de tu negocio", onPick)
        }
    }
}

@Composable
private fun CreateSpaceOption(
    c: KuodraColors,
    useCase: UseCase,
    tone: AvatarTone,
    title: String,
    sub: String,
    onPick: (UseCase) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface2)
            .clickable { onPick(useCase) }
            .padding(horizontal = 15.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTag(spaceTag(useCase), tone, size = 46.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = Kuodra.type.heading, color = c.ink)
            Text(sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

/** Encabezado de hoja inferior: título (Space Grotesk) + botón de cierre. */
@Composable
private fun SheetHeader(c: KuodraColors, title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Kuodra.type.titleScreen, color = c.ink)
        Box(
            Modifier.size(30.dp).clip(Kuodra.shape.pill).background(c.surface2)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { CloseIcon(12.dp, c.ink3) }
    }
}

/** Círculo de selección con palomita (espacio actual). */
@Composable
private fun CheckCircle(c: KuodraColors) {
    Box(Modifier.size(22.dp).clip(Kuodra.shape.pill).background(c.primary),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(11.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.52f)
                lineTo(size.width * 0.42f, size.height * 0.74f)
                lineTo(size.width * 0.82f, size.height * 0.28f)
            }
            drawPath(path, c.primaryInk, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

/** Cruz "✕" de cierre dibujada con dos trazos diagonales. */
@Composable
private fun CloseIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = 2.dp.toPx()
        drawLine(color, Offset(0f, 0f), Offset(w, h), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w, 0f), Offset(0f, h), sw, cap = StrokeCap.Round)
    }
}

/**
 * Menú "Opciones" (botón ···). Réplica del sheet `sheetMenu` del prototipo: encabezado
 * "Opciones" + filas con icono/subtítulo cuya visibilidad depende del caso de uso.
 * El tema oscuro se gestiona desde Ajustes (no aquí).
 */
@Composable
private fun SpaceMenu(
    c: KuodraColors,
    current: UseCase,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit,
    onClosePeriod: () -> Unit,
    onReplenish: () -> Unit,
    onOpenHistory: () -> Unit,
    onLeave: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SheetHeader(c, "Opciones", onClose)
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1 · Compartir resumen/corte (grupo y caja)
            if (current != UseCase.Personal) {
                MenuOptionRow(
                    c = c,
                    iconBg = c.posTint,
                    icon = { ShareGlyph(c.pos) },
                    title = if (current == UseCase.Caja) "Compartir corte" else "Compartir resumen",
                    sub = "Manda el resumen por WhatsApp o PDF",
                    onClick = onShare,
                )
            }
            // 2 · Ajustes (todos; subtítulo contextual)
            MenuOptionRow(
                c = c,
                iconBg = c.tint,
                icon = { SettingsGlyph(c.tintInk) },
                title = "Ajustes",
                sub = when (current) {
                    UseCase.Personal -> "Presupuesto"
                    UseCase.Gastos -> "Nombre, contactos y categorías"
                    UseCase.Caja -> "Nombre, responsable y contactos"
                },
                onClick = onOpenSettings,
            )
            // 3 · Cerrar periodo (solo personal)
            if (current == UseCase.Personal) {
                MenuOptionRow(
                    c = c,
                    rowBg = c.warnTint,
                    iconBg = c.surface,
                    iconBorder = c.warn,
                    icon = { ClosePeriodGlyph(c.warn) },
                    title = "Cerrar periodo",
                    sub = "Quincena 2 · 16–30 jun",
                    onClick = onClosePeriod,
                )
            }
            // 4 · Historial (todos)
            MenuOptionRow(
                c = c,
                iconBg = c.tint,
                icon = { ClockGlyph(c.primary) },
                title = "Historial",
                sub = "Periodos cerrados · reenviar o auditar",
                onClick = onOpenHistory,
            )
            // 5 · Reponer fondo (solo caja)
            if (current == UseCase.Caja) {
                MenuOptionRow(
                    c = c,
                    iconBg = c.warnTint,
                    icon = { TrayGlyph(c.warn) },
                    title = "Reponer fondo",
                    sub = "Sube el saldo sin cerrar el periodo",
                    onClick = onReplenish,
                )
            }
            // 6 · Salir del grupo (solo grupo; con divisor y en rojo)
            if (current == UseCase.Gastos) {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(c.line))
                MenuOptionRow(
                    c = c,
                    rowBg = c.negTint,
                    iconBg = c.surface,
                    icon = { ExitGlyph(c.neg) },
                    title = "Salir del grupo",
                    titleColor = c.neg,
                    sub = "Archívalo y conserva el historial",
                    onClick = onLeave,
                )
            }
        }
    }
}

/** Fila del menú: caja de icono 42dp + título + subtítulo. */
@Composable
private fun MenuOptionRow(
    c: KuodraColors,
    iconBg: Color,
    icon: @Composable () -> Unit,
    title: String,
    sub: String,
    onClick: () -> Unit,
    rowBg: Color = c.surface2,
    iconBorder: Color? = null,
    titleColor: Color = c.ink,
) {
    Row(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(rowBg)
            .clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(Kuodra.shape.md).background(iconBg)
                .then(if (iconBorder != null) Modifier.border(1.dp, iconBorder, Kuodra.shape.md) else Modifier),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(Modifier.weight(1f)) {
            Text(title, style = Kuodra.type.heading, color = titleColor)
            Text(sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

// ---- Iconos de línea del menú (Canvas/Box, mismo estilo que KuodraIcons) ----

/** Burbuja de chat (compartir): círculo con la esquina inferior-izquierda recta. */
@Composable
private fun ShareGlyph(color: Color) {
    Box(Modifier.size(17.dp).border(2.5.dp, color,
        RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 50, bottomStartPercent = 15)))
}

/** Diana de ajustes: anillo con punto central. */
@Composable
private fun SettingsGlyph(color: Color) {
    Box(
        Modifier.size(16.dp).clip(Kuodra.shape.pill).border(2.5.dp, color, Kuodra.shape.pill),
        contentAlignment = Alignment.Center,
    ) { Box(Modifier.size(6.dp).clip(Kuodra.shape.pill).background(color)) }
}

/** Maletín de "cerrar periodo": cuerpo con asa superior. */
@Composable
private fun ClosePeriodGlyph(color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 8.dp, height = 4.dp).border(2.dp, color,
            RoundedCornerShape(topStartPercent = 60, topEndPercent = 60, bottomEndPercent = 0, bottomStartPercent = 0)))
        Box(Modifier.size(width = 16.dp, height = 13.dp).border(2.5.dp, color, Kuodra.shape.sm))
    }
}

/** Reloj (historial): círculo con dos manecillas. */
@Composable
private fun ClockGlyph(color: Color) {
    Box(Modifier.size(17.dp).clip(Kuodra.shape.pill).border(2.5.dp, color, Kuodra.shape.pill)) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val sw = 2.dp.toPx()
            drawLine(color, Offset(cx, cy), Offset(cx, cy - size.height * 0.26f), sw, cap = StrokeCap.Round)
            drawLine(color, Offset(cx, cy), Offset(cx + size.width * 0.2f, cy), sw, cap = StrokeCap.Round)
        }
    }
}

/** Cajón/bandeja (reponer fondo): rectángulo con ranura. */
@Composable
private fun TrayGlyph(color: Color) {
    Box(
        Modifier.size(width = 21.dp, height = 15.dp).border(2.dp, color, Kuodra.shape.sm),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.padding(top = 3.dp).size(width = 7.dp, height = 2.5.dp)
            .clip(Kuodra.shape.pill).background(color))
    }
}

/** Puerta con flecha hacia afuera (salir del grupo). */
@Composable
private fun ExitGlyph(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val sw = 2.5.dp.toPx()
        val w = size.width
        val h = size.height
        val join = androidx.compose.ui.graphics.StrokeJoin.Round
        // marco de puerta (C abierta a la derecha)
        val door = Path().apply {
            moveTo(w * 0.5f, 0f); lineTo(w * 0.08f, 0f); lineTo(w * 0.08f, h); lineTo(w * 0.5f, h)
        }
        drawPath(door, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = join))
        // flecha →
        val cy = h / 2f
        drawLine(color, Offset(w * 0.4f, cy), Offset(w, cy), sw, cap = StrokeCap.Round)
        val arrow = Path().apply {
            moveTo(w * 0.78f, cy - h * 0.18f); lineTo(w, cy); lineTo(w * 0.78f, cy + h * 0.18f)
        }
        drawPath(arrow, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = join))
    }
}

// ---- Sheets disparados desde el menú ----

/** Compartir resumen/corte: opciones PDF / WhatsApp (sin canal real, igual que el reshare). */
@Composable
private fun ShareSheet(c: KuodraColors, current: UseCase, onClose: () -> Unit, onShare: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SheetHeader(c, if (current == UseCase.Caja) "Compartir corte" else "Compartir resumen", onClose)
        Column(
            Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShareButton(c, "Compartir PDF", onShare)
            ShareButton(c, "Enviar por WhatsApp", onShare)
        }
    }
}

@Composable
private fun ShareButton(c: KuodraColors, label: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface2)
            .border(1.dp, c.line, Kuodra.shape.lg).clickable(onClick = onClick).padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Kuodra.type.heading, color = c.ink) }
}

/** Confirmación tras compartir. */
@Composable
private fun ShareDoneSheet(c: KuodraColors, current: UseCase, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).clip(Kuodra.shape.pill).background(c.posTint), contentAlignment = Alignment.Center) {
            Text("✓", style = Kuodra.type.displayAmount, color = c.pos)
        }
        Text(if (current == UseCase.Caja) "Corte enviado" else "Resumen enviado",
            style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(top = 12.dp))
        Text("Se compartió el resumen del periodo.", style = Kuodra.type.caption, color = c.ink2,
            modifier = Modifier.padding(top = 4.dp))
        Box(
            Modifier.fillMaxWidth().padding(top = 18.dp).clip(Kuodra.shape.lg).background(c.primary)
                .clickable(onClick = onClose).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Entendido", style = Kuodra.type.heading, color = c.primaryInk) }
    }
}

/** Cerrar periodo (Personal): confirmación con resumen del periodo. */
@Composable
private fun ClosePeriodSheet(c: KuodraColors, hero: PersonalHero?, onClose: () -> Unit, onConfirm: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SheetHeader(c, "Cerrar periodo", onClose)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Column(Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface2).padding(17.dp)) {
                Text("Periodo actual", style = Kuodra.type.caption, color = c.ink3)
                Text(hero?.budget?.frequencyBadge ?: hero?.caption ?: "Mes en curso",
                    style = Kuodra.type.heading, color = c.ink,
                    modifier = Modifier.padding(top = 4.dp))
                Row(
                    Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(hero?.totalLabel ?: "$0", style = Kuodra.type.titleScreen, color = c.ink)
                    Text("gastado", style = Kuodra.type.caption, color = c.ink3,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            Text("El periodo quedará archivado en el Historial. No podrás editarlo después.",
                style = Kuodra.type.caption, color = c.ink3,
                modifier = Modifier.padding(top = 16.dp, bottom = 18.dp, start = 2.dp, end = 2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.surface2)
                        .border(1.dp, c.line, Kuodra.shape.lg).clickable(onClick = onClose).padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Cancelar", style = Kuodra.type.heading, color = c.ink) }
                Box(
                    Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.primary)
                        .clickable(onClick = onConfirm).padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Cerrar periodo", style = Kuodra.type.heading, color = c.primaryInk) }
            }
        }
    }
}

/** Confirmación tras cerrar el periodo. */
@Composable
private fun ClosePeriodDoneSheet(c: KuodraColors, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).clip(Kuodra.shape.pill).background(c.posTint), contentAlignment = Alignment.Center) {
            Text("✓", style = Kuodra.type.displayAmount, color = c.pos)
        }
        Text("Periodo cerrado", style = Kuodra.type.heading, color = c.ink, modifier = Modifier.padding(top = 12.dp))
        Text("Lo archivamos en el Historial.", style = Kuodra.type.caption, color = c.ink2,
            modifier = Modifier.padding(top = 4.dp))
        Box(
            Modifier.fillMaxWidth().padding(top = 18.dp).clip(Kuodra.shape.lg).background(c.primary)
                .clickable(onClick = onClose).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Entendido", style = Kuodra.type.heading, color = c.primaryInk) }
    }
}

@Composable
private fun LeaveFlow(
    c: KuodraColors,
    step: LeaveStep,
    spaceName: String,
    onSettle: () -> Unit,
    onAdvance: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(c.ink.copy(alpha = 0.45f)).clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = 28.dp).clip(Kuodra.shape.xxl).background(c.surface)
                .border(1.dp, c.line, Kuodra.shape.xxl).padding(22.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (step) {
                LeaveStep.Settle -> {
                    Text("Liquida antes de salir", style = Kuodra.type.heading, color = c.ink)
                    Text("Tienes saldos pendientes con los miembros de $spaceName. Liquida o continúa para archivar.",
                        style = Kuodra.type.caption, color = c.ink2,
                        modifier = Modifier.padding(top = 8.dp))
                    Box(
                        Modifier.fillMaxWidth().padding(top = 16.dp).clip(Kuodra.shape.lg).background(c.primary)
                            .clickable(onClick = onSettle).padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Liquidar ahora", style = Kuodra.type.heading, color = c.primaryInk) }
                    Box(
                        Modifier.fillMaxWidth().padding(top = 10.dp).clip(Kuodra.shape.lg)
                            .border(1.5.dp, c.line, Kuodra.shape.lg)
                            .clickable(onClick = onAdvance).padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Continuar sin liquidar", style = Kuodra.type.heading, color = c.ink2) }
                }
                LeaveStep.Confirm -> {
                    Text("¿Archivar grupo?", style = Kuodra.type.heading, color = c.ink)
                    Text("El grupo pasa a solo lectura para todos. Podrás consultar su historial.",
                        style = Kuodra.type.caption, color = c.ink2, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.weight(1f).clip(Kuodra.shape.lg).border(1.5.dp, c.line, Kuodra.shape.lg)
                                .clickable(onClick = onClose).padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Cancelar", style = Kuodra.type.heading, color = c.ink2) }
                        Box(
                            Modifier.weight(1f).clip(Kuodra.shape.lg).background(c.neg)
                                .clickable(onClick = onAdvance).padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Archivar", style = Kuodra.type.heading, color = c.primaryInk) }
                    }
                }
                LeaveStep.Done -> {
                    Box(Modifier.size(56.dp).clip(Kuodra.shape.pill).background(c.posTint),
                        contentAlignment = Alignment.Center) {
                        Text("✓", style = Kuodra.type.displayAmount, color = c.pos)
                    }
                    Text("Grupo archivado", style = Kuodra.type.heading, color = c.ink,
                        modifier = Modifier.padding(top = 12.dp))
                    Box(
                        Modifier.fillMaxWidth().padding(top = 16.dp).clip(Kuodra.shape.lg).background(c.primary)
                            .clickable(onClick = onClose).padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Entendido", style = Kuodra.type.heading, color = c.primaryInk) }
                }
                LeaveStep.None -> {}
            }
        }
    }
}

private fun spaceTitle(useCase: UseCase): String = when (useCase) {
    UseCase.Personal -> "Mis gastos"
    UseCase.Gastos -> "Casa Roma"
    UseCase.Caja -> "Caja Changarro"
}

/** Subtítulo en el selector de espacios (réplica de `baseSpaces` del prototipo). */
private fun spaceListSub(useCase: UseCase): String = when (useCase) {
    UseCase.Personal -> "Solo tú"
    UseCase.Gastos -> "4 miembros"
    UseCase.Caja -> "Responsable"
}

private fun spaceTag(useCase: UseCase): String = when (useCase) {
    UseCase.Personal -> "Mi"
    UseCase.Gastos -> "Ca"
    UseCase.Caja -> "Cj"
}

/** Tono de avatar por tipo de espacio (personal=tint, grupo=pos, caja=warn). */
private fun spaceTone(useCase: UseCase): AvatarTone = when (useCase) {
    UseCase.Personal -> AvatarTone.Tint
    UseCase.Gastos -> AvatarTone.Pos
    UseCase.Caja -> AvatarTone.Warn
}

@Composable
private fun SectionHeader(c: KuodraColors, title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Kuodra.type.heading, color = c.ink)
        if (onSeeAll != null) {
            Text("Ver todo", style = Kuodra.type.caption, color = c.primary,
                modifier = Modifier.clickable(onClick = onSeeAll))
        }
    }
}

@Composable
private fun ReminderBanner(c: KuodraColors) {
    Row(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.warnTint)
            .border(1.dp, c.warn.copy(alpha = 0.25f), Kuodra.shape.lg)
            .padding(horizontal = 15.dp, vertical = 13.dp)
            .padding(bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(Kuodra.shape.md).background(c.surface),
            contentAlignment = Alignment.Center) {
            Text("⏰", style = Kuodra.type.caption, color = c.warn)
        }
        Column(Modifier.weight(1f)) {
            Text("Llevas 27 días sin liquidar junio", style = Kuodra.type.caption, color = c.ink)
            Text("Buen momento para saldar con los demás miembros",
                style = Kuodra.type.caption, color = c.ink2, modifier = Modifier.padding(top = 1.dp))
        }
        Chevron(8.dp, c.warn, degrees = 0f)
    }
}

@Composable
private fun LowFundBanner(c: KuodraColors, onReplenish: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.warnTint)
            .border(1.dp, c.warn.copy(alpha = 0.25f), Kuodra.shape.lg)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(Kuodra.shape.md).background(c.surface),
            contentAlignment = Alignment.Center) {
            Text("💵", style = Kuodra.type.caption, color = c.warn)
        }
        Column(Modifier.weight(1f)) {
            Text("Tu caja está baja", style = Kuodra.type.caption, color = c.ink)
            Text("Quedan $900 (18%) del fondo de $5,000",
                style = Kuodra.type.caption, color = c.ink2, modifier = Modifier.padding(top = 1.dp))
        }
        Box(
            Modifier.clip(Kuodra.shape.md).background(c.warn)
                .clickable(onClick = onReplenish)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) { Text("Reponer", style = Kuodra.type.caption, color = Color.White) }
    }
}

@Composable
private fun DashboardHero(c: KuodraColors, uc: UseCase, heroLabel: String, personalHero: PersonalHero?) {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .clip(Kuodra.shape.xxl).background(c.primary)
            .padding(22.dp),
    ) {
        if (uc == UseCase.Personal && personalHero != null) {
            val budget = personalHero.budget
            if (budget != null) {
                Box(
                    Modifier.clip(Kuodra.shape.pill).background(c.primaryInk.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) { Text(budget.frequencyBadge, style = Kuodra.type.caption, color = c.primaryInk) }
                Spacer(Modifier.height(11.dp))
            }
            Text(personalHero.caption, style = Kuodra.type.caption, color = c.primaryInk.copy(alpha = 0.85f))
            Text(personalHero.totalLabel, style = Kuodra.type.displayAmount, color = c.primaryInk,
                modifier = Modifier.padding(top = 6.dp))
            if (budget != null) {
                HeroProgress(c, budget.progressLabel, budget.rightLabel, budget.pct)
                Row(
                    Modifier.padding(top = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(Modifier.size(7.dp).clip(Kuodra.shape.pill)
                        .background(if (budget.onTrack) c.pos else c.warn))
                    Text(budget.paceText, style = Kuodra.type.caption, color = c.primaryInk)
                    Text(budget.paceDetail, style = Kuodra.type.overline, color = c.primaryInk.copy(alpha = 0.82f))
                }
            }
            return@Column
        }

        // Gastos / Caja (aún hardcodeado; fuera de alcance de esta versión)
        Text(heroLabel, style = Kuodra.type.caption, color = c.primaryInk.copy(alpha = 0.85f))
        Text(
            when (uc) {
                UseCase.Gastos -> "+$890"
                UseCase.Caja -> "$900"
                UseCase.Personal -> "$0"
            },
            style = Kuodra.type.displayAmount, color = c.primaryInk,
            modifier = Modifier.padding(top = 6.dp),
        )
        when (uc) {
            UseCase.Gastos -> {
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroStat(c, "Te deben", "$1,090", Modifier.weight(1f))
                    HeroStat(c, "Debes", "$200", Modifier.weight(1f))
                }
            }
            UseCase.Caja -> HeroProgress(c, "$900 disponibles de $5,000", "18%", 0.18f)
            UseCase.Personal -> {}
        }
    }
}

@Composable
private fun HeroStat(c: KuodraColors, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(Kuodra.shape.md).background(c.primaryInk.copy(alpha = 0.14f))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Text(label, style = Kuodra.type.caption, color = c.primaryInk.copy(alpha = 0.8f))
        Text(value, style = Kuodra.type.heading, color = c.primaryInk, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun HeroProgress(c: KuodraColors, label: String, right: String, pct: Float) {
    Column(Modifier.padding(top = 18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = Kuodra.type.caption, color = c.primaryInk.copy(alpha = 0.9f))
            Text(right, style = Kuodra.type.caption, color = c.primaryInk)
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 7.dp).height(9.dp)
                .clip(Kuodra.shape.pill).background(c.primaryInk.copy(alpha = 0.22f)),
        ) {
            Box(Modifier.fillMaxWidth(pct).height(9.dp).clip(Kuodra.shape.pill).background(Color.White))
        }
    }
}

@Composable
private fun SettleAction(c: KuodraColors, uc: UseCase, onSettle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface)
            .border(1.dp, c.line, Kuodra.shape.lg)
            .clickable(onClick = onSettle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(Kuodra.shape.md).background(c.tint),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 14.dp, height = 2.5.dp).clip(Kuodra.shape.pill).background(c.tintInk))
        }
        Column(Modifier.weight(1f)) {
            Text(if (uc == UseCase.Gastos) "Liquidar junio" else "Hacer corte de caja",
                style = Kuodra.type.caption, color = c.ink)
            Text(if (uc == UseCase.Gastos) "Salda con los demás miembros"
                 else "Cierra el periodo y concilia la caja",
                style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
        }
        Chevron(8.dp, c.ink3)
    }
}

@Composable
private fun PeopleCard(c: KuodraColors, people: List<Person>) {
    Column(
        Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface)
            .border(1.dp, c.line, Kuodra.shape.xl),
    ) {
        people.forEachIndexed { i, p ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToneAvatar(p.initials, p.tone)
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = Kuodra.type.body, color = c.ink)
                    Text(p.sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
                }
                Text(p.amount, style = Kuodra.type.heading,
                    color = when (p.positive) { true -> c.pos; false -> c.neg; null -> c.ink })
            }
            if (i < people.lastIndex) Divider(c)
        }
    }
}

@Composable
private fun CategoriesCard(c: KuodraColors, categories: List<CategoryBreakdown>) {
    Column(
        Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface)
            .border(1.dp, c.line, Kuodra.shape.xl),
    ) {
        if (categories.isEmpty()) {
            EmptyRow(c, "Aún no hay gastos", "El desglose por categoría aparecerá al registrar gastos.")
            return@Column
        }
        categories.forEachIndexed { i, cat ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryTag(cat.tag, cat.tone)
                    Column(Modifier.weight(1f)) {
                        Text(cat.name, style = Kuodra.type.body, color = c.ink)
                        Text(cat.sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
                    }
                    Text(cat.amount, style = Kuodra.type.heading, color = c.ink)
                }
                Box(
                    Modifier.fillMaxWidth().padding(start = 53.dp, top = 10.dp).height(6.dp)
                        .clip(Kuodra.shape.pill).background(c.surface2),
                ) {
                    Box(Modifier.fillMaxWidth(cat.pct).height(6.dp).clip(Kuodra.shape.pill)
                        .background(c.avatarInkColor(cat.tone)))
                }
            }
            if (i < categories.lastIndex) Divider(c)
        }
    }
}

@Composable
private fun MovementsCard(
    c: KuodraColors,
    movements: List<MovementUi>,
    onOpenMovement: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface)
            .border(1.dp, c.line, Kuodra.shape.xl),
    ) {
        if (movements.isEmpty()) {
            EmptyRow(c, "Sin movimientos", "Toca Agregar para registrar tu primer gasto.")
            return@Column
        }
        movements.forEachIndexed { i, m ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpenMovement(m.id) }
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryTag(m.catTag, m.tone)
                Column(Modifier.weight(1f)) {
                    Text(m.title, style = Kuodra.type.body, color = c.ink)
                    Text(m.meta, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
                }
                Text(m.amount, style = Kuodra.type.heading, color = c.ink)
                Spacer(Modifier.width(2.dp))
                Chevron(7.dp, c.ink3)
            }
            if (i < movements.lastIndex) Divider(c)
        }
    }
}

@Composable
private fun Divider(c: KuodraColors) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
}

/** Estado vacío dentro de una tarjeta: título + ayuda. */
@Composable
private fun EmptyRow(c: KuodraColors, title: String, hint: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
        Text(title, style = Kuodra.type.body, color = c.ink2)
        Text(hint, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 3.dp))
    }
}

/** Color de la tinta del tono (para barras de categoría). */
private fun KuodraColors.avatarInkColor(tone: AvatarTone): Color = when (tone) {
    AvatarTone.Tint -> tintInk
    AvatarTone.Pos -> pos
    AvatarTone.Warn -> warn
    AvatarTone.Neg -> neg
}
