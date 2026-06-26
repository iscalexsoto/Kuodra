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
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.UseCase
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

                DashboardHero(c, uc, t.heroLabel)

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

                SectionHeader(c, "Movimientos recientes", onSeeAll = onSeeAllMovements)
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
                darkTheme = overlay.darkTheme,
                onOpenSettings = { viewModel.onCloseSheet(); onOpenSettings() },
                onReplenish = { viewModel.onCloseSheet(); onReplenish() },
                onOpenHistory = { viewModel.onCloseSheet(); onOpenHistory() },
                onToggleTheme = viewModel::onToggleTheme,
                onLeave = viewModel::onLeaveStart,
            )
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

@Composable
private fun SpaceMenu(
    c: KuodraColors,
    current: UseCase,
    darkTheme: Boolean,
    onOpenSettings: () -> Unit,
    onReplenish: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleTheme: () -> Unit,
    onLeave: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        MenuAction(c, "Ajustes del espacio", onOpenSettings)
        if (current == UseCase.Caja) MenuAction(c, "Reponer fondo", onReplenish)
        MenuAction(c, "Historial de cortes", onOpenHistory)

        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp).clip(Kuodra.shape.lg)
                .clickable(onClick = onToggleTheme).padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Tema oscuro", style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
            Box(
                Modifier.width(46.dp).height(28.dp).clip(Kuodra.shape.pill)
                    .background(if (darkTheme) c.primary else c.surface2)
                    .border(1.dp, if (darkTheme) c.primary else c.line, Kuodra.shape.pill),
                contentAlignment = if (darkTheme) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(Modifier.padding(horizontal = 3.dp).size(22.dp).clip(Kuodra.shape.pill)
                    .background(if (darkTheme) c.primaryInk else c.surface))
            }
        }

        if (current == UseCase.Gastos) {
            Box(
                Modifier.fillMaxWidth().padding(top = 6.dp).clip(Kuodra.shape.lg).background(c.negTint)
                    .clickable(onClick = onLeave).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Salir del grupo", style = Kuodra.type.heading, color = c.neg) }
        }
    }
}

@Composable
private fun MenuAction(c: KuodraColors, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Kuodra.type.body, color = c.ink, modifier = Modifier.weight(1f))
        Chevron(7.dp, c.ink3)
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
private fun DashboardHero(c: KuodraColors, uc: UseCase, heroLabel: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .clip(Kuodra.shape.xxl).background(c.primary)
            .padding(22.dp),
    ) {
        if (uc == UseCase.Personal) {
            Box(
                Modifier.clip(Kuodra.shape.pill).background(c.primaryInk.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) { Text("Quincenal · 1 y 16", style = Kuodra.type.caption, color = c.primaryInk) }
            Spacer(Modifier.height(11.dp))
        }
        Text(heroLabel, style = Kuodra.type.caption, color = c.primaryInk.copy(alpha = 0.85f))
        Text(
            when (uc) {
                UseCase.Gastos -> "+$890"
                UseCase.Caja -> "$900"
                UseCase.Personal -> "$980"
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
            UseCase.Personal -> {
                HeroProgress(c, "$980 de $6,000 presupuesto", "16%", 0.16f)
                Row(
                    Modifier.padding(top = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(Modifier.size(7.dp).clip(Kuodra.shape.pill).background(c.pos))
                    Text("Vas a buen ritmo", style = Kuodra.type.caption, color = c.primaryInk)
                    Text("16% del presupuesto · 33% del periodo",
                        style = Kuodra.type.overline, color = c.primaryInk.copy(alpha = 0.82f))
                }
            }
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
private fun CategoriesCard(c: KuodraColors, categories: List<Category>) {
    Column(
        Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface)
            .border(1.dp, c.line, Kuodra.shape.xl),
    ) {
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
    movements: List<com.arenacun.kuodra.domain.model.Movement>,
    onOpenMovement: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(Kuodra.shape.xl).background(c.surface)
            .border(1.dp, c.line, Kuodra.shape.xl),
    ) {
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

/** Color de la tinta del tono (para barras de categoría). */
private fun KuodraColors.avatarInkColor(tone: AvatarTone): Color = when (tone) {
    AvatarTone.Tint -> tintInk
    AvatarTone.Pos -> pos
    AvatarTone.Warn -> warn
    AvatarTone.Neg -> neg
}
