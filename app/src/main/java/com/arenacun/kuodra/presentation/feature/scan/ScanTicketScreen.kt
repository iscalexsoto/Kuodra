package com.arenacun.kuodra.presentation.feature.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arenacun.kuodra.R
import com.arenacun.kuodra.domain.scan.ScanSource
import com.arenacun.kuodra.domain.scan.TicketScan
import com.arenacun.kuodra.presentation.component.BackCircle
import com.arenacun.kuodra.presentation.component.KIcon
import com.arenacun.kuodra.presentation.component.KuodraButton
import com.arenacun.kuodra.presentation.component.KuodraButtonVariant
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraColors
import java.io.File

/**
 * Pantalla de escaneo: pide permiso y muestra la cámara (fuente cámara) o lanza el Photo Picker
 * (fuente galería, sin permisos en minSdk 33), corre el análisis con la animación "Escaneando" y
 * entrega el [TicketScan] por [onScanned]. Errores y permisos denegados ofrecen reintentar o caer
 * a la captura manual.
 */
@Composable
fun ScanTicketScreen(
    viewModel: ScanTicketViewModel,
    onBack: () -> Unit,
    onScanned: (TicketScan) -> Unit,
    onFallbackToManual: () -> Unit,
) {
    val c = Kuodra.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.scanned.collect { onScanned(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::onPermissionResult,
    )
    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) onBack() else viewModel.onImageReady(uri.toString())
    }

    // Las fases "de sistema" disparan su launcher al entrar (también al reintentar).
    LaunchedEffect(state.phase) {
        when (state.phase) {
            ScanPhase.RequestingPermission -> {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.onPermissionResult(true)
                else permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            ScanPhase.PickingImage ->
                pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            else -> {}
        }
    }

    Box(Modifier.fillMaxSize().background(c.screenBg)) {
        when (state.phase) {
            // Esperando la respuesta del sistema (diálogo de permiso / Photo Picker): fondo neutro.
            ScanPhase.RequestingPermission, ScanPhase.PickingImage -> {}
            ScanPhase.Camera -> CameraContent(
                c = c,
                onCaptured = viewModel::onImageReady,
                onCaptureFailed = viewModel::onCaptureFailed,
            )
            ScanPhase.PermissionDenied -> MessageContent(
                c = c,
                title = "Necesitamos la cámara",
                body = "Para escanear tu ticket hace falta permiso de cámara. También puedes capturar el movimiento a mano.",
                primaryLabel = "Permitir cámara",
                onPrimary = viewModel::onRetryPermission,
                onManual = onFallbackToManual,
            )
            ScanPhase.Analyzing -> AnalyzingContent(c)
            ScanPhase.Error -> MessageContent(
                c = c,
                title = "No pudimos leer el ticket",
                body = "La foto no se pudo analizar. Intenta de nuevo con buena luz y el ticket plano, o captura el movimiento a mano.",
                primaryLabel = if (state.source == ScanSource.Camera) "Reintentar" else "Elegir otra foto",
                onPrimary = viewModel::onRetry,
                onManual = onFallbackToManual,
            )
        }

        // Atrás disponible salvo durante el análisis (no hay nada que cancelar a medias).
        if (state.phase != ScanPhase.Analyzing) {
            BackCircle(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 10.dp),
            )
        }
    }
}

// ---- Cámara (CameraX) ----

@Composable
private fun CameraContent(
    c: KuodraColors,
    onCaptured: (String) -> Unit,
    onCaptureFailed: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    // Guarda el provider para desconectar la cámara al salir de la fase/pantalla.
    val providerHolder = remember { arrayOfNulls<ProcessCameraProvider>(1) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        // La cámara puede no estar disponible (ocupada, sin hardware, emulador):
                        // eso es la fase Error de la pantalla, nunca un crash.
                        runCatching {
                            val provider = future.get()
                            providerHolder[0] = provider
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                            )
                        }.onFailure { onCaptureFailed() }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
        )
        DisposableEffect(Unit) {
            onDispose { providerHolder[0]?.unbindAll() }
        }

        Text(
            "Encuadra el ticket completo",
            style = Kuodra.type.body,
            color = c.primaryInk,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
        )

        // Botón de captura
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .size(74.dp)
                .clip(Kuodra.shape.pill)
                .background(c.primary)
                .border(4.dp, c.primaryInk.copy(alpha = 0.35f), Kuodra.shape.pill)
                .clickable {
                    runCatching {
                        val file = File.createTempFile("scan_", ".jpg", context.cacheDir)
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onCaptured(Uri.fromFile(file).toString())
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    onCaptureFailed()
                                }
                            },
                        )
                    }.onFailure { onCaptureFailed() }
                },
            contentAlignment = Alignment.Center,
        ) { KIcon(R.drawable.ic_camera, 28.dp, c.primaryInk) }
    }
}

// ---- Estado "Escaneando" (animación del ticket) ----

@Composable
private fun AnalyzingContent(c: KuodraColors) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TicketScanningAnimation(c)
        Spacer(Modifier.height(28.dp))
        Text("Escaneando ticket…", style = Kuodra.type.titleScreen, color = c.ink)
        Text(
            "Leyendo el comercio, el total y las partidas",
            style = Kuodra.type.body,
            color = c.ink3,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * Silueta de ticket con una línea de escaneo que la recorre de arriba a abajo en bucle.
 * Todo con tokens del tema (funciona en claro y oscuro).
 */
@Composable
private fun TicketScanningAnimation(c: KuodraColors) {
    val ticketWidth = 190.dp
    val ticketHeight = 250.dp
    val beamHeight = 52.dp

    val transition = rememberInfiniteTransition(label = "scanBeam")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_400), RepeatMode.Reverse),
        label = "scanBeamProgress",
    )

    Box(
        Modifier
            .width(ticketWidth)
            .height(ticketHeight)
            .clip(Kuodra.shape.lg)
            .background(c.surface)
            .border(1.dp, c.line, Kuodra.shape.lg),
    ) {
        // "Texto" del ticket: líneas grises de anchos variados.
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FakeLine(c, widthFraction = 0.55f, tall = true)
            FakeLine(c, widthFraction = 0.35f)
            Spacer(Modifier.height(4.dp))
            FakeLine(c, widthFraction = 1f)
            FakeLine(c, widthFraction = 0.85f)
            FakeLine(c, widthFraction = 0.9f)
            FakeLine(c, widthFraction = 0.7f)
            Spacer(Modifier.height(4.dp))
            FakeLine(c, widthFraction = 0.5f, tall = true)
        }

        // Línea de escaneo: gradiente vertical que barre el ticket.
        Box(
            Modifier
                .fillMaxWidth()
                .height(beamHeight)
                .offset { IntOffset(0, ((ticketHeight - beamHeight) * progress).roundToPx()) }
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            c.primary.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset { IntOffset(0, ((ticketHeight - beamHeight) * progress + beamHeight / 2).roundToPx()) }
                .background(c.primary),
        )
    }
}

@Composable
private fun FakeLine(c: KuodraColors, widthFraction: Float, tall: Boolean = false) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(if (tall) 14.dp else 9.dp)
            .clip(Kuodra.shape.pill)
            .background(c.surface2),
    )
}

// ---- Permiso denegado / error de análisis ----

@Composable
private fun MessageContent(
    c: KuodraColors,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onManual: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).clip(Kuodra.shape.lg).background(c.tint),
            contentAlignment = Alignment.Center,
        ) { KIcon(R.drawable.ic_camera, 30.dp, c.tintInk) }
        Spacer(Modifier.height(18.dp))
        Text(title, style = Kuodra.type.titleScreen, color = c.ink, textAlign = TextAlign.Center)
        Text(
            body,
            style = Kuodra.type.body,
            color = c.ink3,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        KuodraButton(text = primaryLabel, onClick = onPrimary, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        KuodraButton(
            text = "Capturar manualmente",
            onClick = onManual,
            variant = KuodraButtonVariant.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
