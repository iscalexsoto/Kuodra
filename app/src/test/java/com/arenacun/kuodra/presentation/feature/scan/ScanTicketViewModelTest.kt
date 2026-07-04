package com.arenacun.kuodra.presentation.feature.scan

import com.arenacun.kuodra.MainDispatcherRule
import com.arenacun.kuodra.domain.scan.OcrEngine
import com.arenacun.kuodra.domain.scan.RegexTicketParser
import com.arenacun.kuodra.domain.scan.ScanSource
import com.arenacun.kuodra.domain.telemetry.NoOpTelemetry
import com.arenacun.kuodra.domain.usecase.ScanTicketUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanTicketViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeOcrEngine(var result: Result<String> = Result.success("OXXO\nTOTAL 59.50")) : OcrEngine {
        override suspend fun recognize(imageUri: String): Result<String> = result
    }

    private fun viewModel(
        source: ScanSource = ScanSource.Camera,
        ocr: FakeOcrEngine = FakeOcrEngine(),
    ) = ScanTicketViewModel(
        source = source,
        scanTicket = ScanTicketUseCase(ocr, listOf(RegexTicketParser()), NoOpTelemetry),
        telemetry = NoOpTelemetry,
    )

    @Test
    fun `camera source starts requesting permission and gallery goes straight to the picker`() {
        assertEquals(ScanPhase.RequestingPermission, viewModel(ScanSource.Camera).uiState.value.phase)
        assertEquals(ScanPhase.PickingImage, viewModel(ScanSource.Gallery).uiState.value.phase)
    }

    @Test
    fun `permission result routes to camera or denied`() {
        val vm = viewModel()
        vm.onPermissionResult(true)
        assertEquals(ScanPhase.Camera, vm.uiState.value.phase)

        vm.onPermissionResult(false)
        assertEquals(ScanPhase.PermissionDenied, vm.uiState.value.phase)

        vm.onRetryPermission()
        assertEquals(ScanPhase.RequestingPermission, vm.uiState.value.phase)
    }

    @Test
    fun `a ready image analyzes and emits the scanned event`() = runTest {
        val vm = viewModel()
        vm.onPermissionResult(true)

        val scanned = async { vm.scanned.first() }
        vm.onImageReady("file://ticket.jpg")
        assertEquals(ScanPhase.Analyzing, vm.uiState.value.phase)
        advanceUntilIdle()

        val scan = scanned.await()
        assertEquals("OXXO", scan.parsed.merchant)
        assertEquals(ScanSource.Camera, scan.scanSource)
    }

    @Test
    fun `ocr failure lands on the error phase and retry returns to the camera`() = runTest {
        val ocr = FakeOcrEngine(Result.failure(RuntimeException("sin texto")))
        val vm = viewModel(ocr = ocr)
        vm.onPermissionResult(true)

        vm.onImageReady("file://ticket.jpg")
        advanceUntilIdle()
        assertEquals(ScanPhase.Error, vm.uiState.value.phase)

        vm.onRetry()
        assertEquals(ScanPhase.Camera, vm.uiState.value.phase)
    }

    @Test
    fun `gallery retry relaunches the picker and capture failure errors out`() = runTest {
        val vm = viewModel(ScanSource.Gallery, FakeOcrEngine(Result.failure(RuntimeException("x"))))
        vm.onImageReady("content://media/9")
        advanceUntilIdle()
        assertEquals(ScanPhase.Error, vm.uiState.value.phase)

        vm.onRetry()
        assertEquals(ScanPhase.PickingImage, vm.uiState.value.phase)

        val camVm = viewModel()
        camVm.onCaptureFailed()
        assertEquals(ScanPhase.Error, camVm.uiState.value.phase)
    }
}
