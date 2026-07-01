package com.arenacun.kuodra.presentation.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.presentation.app.StartState
import com.arenacun.kuodra.presentation.app.resolveStartState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    /** Evento de una sola vez: el código se solicitó con éxito ⇒ navegar a la pantalla de OTP. */
    private val _otpSent = Channel<Unit>(Channel.BUFFERED)
    val otpSent = _otpSent.receiveAsFlow()

    /**
     * Evento de una sola vez: el código fue verificado correctamente. Lleva el destino ya
     * resuelto (nombre/onboarding/dashboard) según la sesión recuperada del backend, para
     * no re-preguntar el nombre en un dispositivo nuevo cuando el usuario ya lo tiene.
     */
    private val _otpVerified = Channel<StartState>(Channel.BUFFERED)
    val otpVerified = _otpVerified.receiveAsFlow()

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(email = value, emailValid = authRepository.isValidEmail(value), requestError = null)
        }
    }

    /** Da de alta-si-falta y solicita el código; solo navega a OTP si el envío tuvo éxito. */
    fun requestOtp() {
        val state = _uiState.value
        if (state.requesting || !state.emailValid) return
        viewModelScope.launch {
            _uiState.update { it.copy(requesting = true, requestError = null) }
            val result = authRepository.requestOtp(state.email)
            _uiState.update { it.copy(requesting = false) }
            if (result.isSuccess) {
                _uiState.update { it.copy(otp = "", otpError = false) }
                _otpSent.send(Unit)
            } else {
                _uiState.update {
                    it.copy(requestError = "No pudimos enviar el código. Revisa tu conexión e inténtalo de nuevo.")
                }
            }
        }
    }

    fun onOtpDigit(digit: String) {
        val current = _uiState.value
        if (current.verifying || current.otp.length >= 6) return
        val next = current.otp + digit
        _uiState.update { it.copy(otp = next, otpError = false) }
        if (next.length == 6) verify(next)
    }

    fun onOtpBackspace() {
        _uiState.update { if (it.otp.isEmpty()) it else it.copy(otp = it.otp.dropLast(1)) }
    }

    private fun verify(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(verifying = true) }
            val result = authRepository.verifyOtp(code)
            _uiState.update { it.copy(verifying = false) }
            if (result.isSuccess) {
                _uiState.update { it.copy(otpError = false) }
                // La sesión era null antes del OTP (dispositivo nuevo o tras signOut), así que
                // esperamos a la sesión recién guardada (con el `name` del backend) y resolvemos
                // el destino con la misma lógica que el arranque.
                val session = authRepository.session.filterNotNull().first()
                val next = resolveStartState(session, spaceRepository.isConfigured())
                delay(400)
                _otpVerified.send(next)
                _uiState.update { it.copy(otp = "") }
            } else {
                _uiState.update { it.copy(otpError = true) }
                delay(1200)
                _uiState.update { it.copy(otp = "", otpError = false) }
            }
        }
    }
}
