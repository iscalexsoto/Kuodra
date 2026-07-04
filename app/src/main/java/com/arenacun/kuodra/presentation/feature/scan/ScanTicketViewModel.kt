package com.arenacun.kuodra.presentation.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.scan.ScanSource
import com.arenacun.kuodra.domain.scan.TicketScan
import com.arenacun.kuodra.domain.telemetry.Telemetry
import com.arenacun.kuodra.domain.usecase.ScanTicketUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pantalla de escaneo de ticket. Orquesta las fases (permiso → cámara/picker → analizando →
 * error) y corre [ScanTicketUseCase] sobre la imagen elegida. El resultado sale por el evento
 * one-shot [scanned]; la navegación (y el guardado del draft) se decide en el NavHost.
 */
class ScanTicketViewModel(
    private val source: ScanSource,
    private val scanTicket: ScanTicketUseCase,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScanTicketUiState(
            source = source,
            phase = when (source) {
                ScanSource.Camera -> ScanPhase.RequestingPermission
                ScanSource.Gallery -> ScanPhase.PickingImage
            },
        ),
    )
    val uiState: StateFlow<ScanTicketUiState> = _uiState.asStateFlow()

    private val _scanned = Channel<TicketScan>(Channel.BUFFERED)
    val scanned = _scanned.receiveAsFlow()

    init {
        telemetry.breadcrumb("scan", "scan opened", mapOf("source" to source.name))
    }

    fun onPermissionResult(granted: Boolean) = _uiState.update {
        it.copy(phase = if (granted) ScanPhase.Camera else ScanPhase.PermissionDenied)
    }

    /** Vuelve a pedir el permiso (la pantalla relanza el launcher al ver la fase). */
    fun onRetryPermission() = _uiState.update { it.copy(phase = ScanPhase.RequestingPermission) }

    /**
     * Imagen lista (captura de CameraX o Uri del Photo Picker): analiza. La fase Analyzing dura
     * al menos [MIN_ANALYZING_MS] para que la animación de escaneo se perciba, aunque el OCR
     * local termine antes.
     */
    fun onImageReady(uriString: String) {
        _uiState.update { it.copy(phase = ScanPhase.Analyzing) }
        viewModelScope.launch {
            val minAnimation = launch { delay(MIN_ANALYZING_MS) }
            val result = scanTicket(uriString, source)
            minAnimation.join()
            result
                .onSuccess { _scanned.send(it) }
                .onFailure { _uiState.update { st -> st.copy(phase = ScanPhase.Error) } }
        }
    }

    /** La captura de CameraX falló (disco/hardware): tratar como error de análisis. */
    fun onCaptureFailed() = _uiState.update { it.copy(phase = ScanPhase.Error) }

    /** Reintentar tras un error: volver a la cámara o relanzar el picker según la fuente. */
    fun onRetry() = _uiState.update {
        it.copy(
            phase = when (source) {
                ScanSource.Camera -> ScanPhase.Camera
                ScanSource.Gallery -> ScanPhase.PickingImage
            },
        )
    }

    private companion object {
        const val MIN_ANALYZING_MS = 1_200L
    }
}
