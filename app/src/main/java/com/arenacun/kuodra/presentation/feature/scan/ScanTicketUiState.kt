package com.arenacun.kuodra.presentation.feature.scan

import com.arenacun.kuodra.domain.scan.ScanSource

/**
 * Fase de la pantalla de escaneo:
 * - [RequestingPermission] esperando la respuesta del permiso CAMERA (solo fuente cámara).
 * - [PermissionDenied] rationale con reintentar o caer a captura manual.
 * - [Camera] preview CameraX con botón de captura.
 * - [PickingImage] el Photo Picker del sistema está abierto (fuente galería; sin permisos).
 * - [Analyzing] OCR + parseo en curso: animación del ticket escaneándose.
 * - [Error] el análisis falló (OCR sin texto, imagen ilegible): reintentar o capturar manual.
 */
enum class ScanPhase { RequestingPermission, PermissionDenied, Camera, PickingImage, Analyzing, Error }

data class ScanTicketUiState(
    val source: ScanSource,
    val phase: ScanPhase,
)
