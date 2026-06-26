package com.arenacun.kuodra.presentation.feature.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.terminologyFor
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.CategoryTag
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CreateSpaceScreen(
    useCase: UseCase,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateSpaceViewModel = koinViewModel { parametersOf(useCase) },
) {
    val c = Kuodra.colors
    val name by viewModel.name.collectAsStateWithLifecycle()
    val defaultName = terminologyFor(useCase).groupName
    val title = when (useCase) {
        UseCase.Personal -> "Crea tu espacio"
        UseCase.Caja -> "Crea tu fondo"
        UseCase.Gastos -> "Crea tu grupo"
    }
    val label = when (useCase) {
        UseCase.Personal -> "NOMBRE DEL ESPACIO"
        UseCase.Caja -> "NOMBRE DEL FONDO"
        UseCase.Gastos -> "NOMBRE DEL GRUPO"
    }

    Column(
        Modifier.fillMaxSize().background(c.screenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 20.dp),
        ) {
            BackCircle(onClick = onBack)
            Text(title, style = Kuodra.type.heading, color = c.ink)
        }

        Text(label, style = Kuodra.type.overline, color = c.ink3,
            modifier = Modifier.padding(start = 2.dp, bottom = 9.dp))
        Box(
            Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface)
                .border(1.5.dp, c.primary, Kuodra.shape.lg)
                .padding(horizontal = 16.dp, vertical = 15.dp),
        ) {
            BasicTextField(
                value = name,
                onValueChange = viewModel::onNameChange,
                singleLine = true,
                textStyle = Kuodra.type.heading.copy(color = c.ink),
                cursorBrush = SolidColor(c.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (name.isEmpty()) {
                        Text(defaultName, style = Kuodra.type.heading, color = c.ink3)
                    }
                    inner()
                },
            )
        }

        Text("EMPIEZA CON TUS DATOS · OPCIONAL", style = Kuodra.type.overline, color = c.ink3,
            modifier = Modifier.padding(start = 2.dp, top = 24.dp, bottom = 9.dp))
        ImportRow(c, AvatarTone.Pos, "Importar desde Excel / Sheets", "La IA mapea tus columnas por ti")
        Spacer(Modifier.padding(top = 10.dp))
        ImportRow(c, AvatarTone.Tint, "Escanear tu primer ticket", "Empieza capturando un gasto al instante")

        Spacer(Modifier.padding(top = 20.dp))
        Box(
            Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.primary)
                .clickable {
                    viewModel.create()
                    onCreated()
                }
                .padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Crear y empezar", style = Kuodra.type.heading, color = c.primaryInk)
        }
        Text("Puedes configurar las integraciones después", style = Kuodra.type.caption, color = c.ink3,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun ImportRow(
    c: KuodraColors,
    tone: AvatarTone,
    title: String,
    sub: String,
) {
    Row(
        Modifier.fillMaxWidth().clip(Kuodra.shape.lg).background(c.surface)
            .border(1.5.dp, c.line, Kuodra.shape.lg)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTag("∎", tone, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = Kuodra.type.caption, color = c.ink)
            Text(sub, style = Kuodra.type.caption, color = c.ink3, modifier = Modifier.padding(top = 1.dp))
        }
        Box(
            Modifier.clip(RoundedCornerShape(6.dp)).background(c.tint)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) { Text("IA", style = Kuodra.type.overline, color = c.tintInk) }
    }
}
