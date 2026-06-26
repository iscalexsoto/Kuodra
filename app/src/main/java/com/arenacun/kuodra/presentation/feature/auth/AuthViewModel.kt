package com.arenacun.kuodra.presentation.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    /** Evento de una sola vez: el código fue verificado correctamente. */
    private val _otpVerified = Channel<Unit>(Channel.BUFFERED)
    val otpVerified = _otpVerified.receiveAsFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailValid = authRepository.isValidEmail(value)) }
    }

    /** Llamado al pasar a la pantalla de código. */
    fun requestOtp() {
        val email = _uiState.value.email
        viewModelScope.launch { authRepository.requestOtp(email) }
    }

    fun onOtpDigit(digit: String) {
        val current = _uiState.value.otp
        if (current.length >= 6) return
        val next = current + digit
        _uiState.update { it.copy(otp = next) }
        if (next.length == 6) verify(next)
    }

    fun onOtpBackspace() {
        _uiState.update { if (it.otp.isEmpty()) it else it.copy(otp = it.otp.dropLast(1)) }
    }

    private fun verify(code: String) {
        viewModelScope.launch {
            val result = authRepository.verifyOtp(code)
            if (result.isSuccess) {
                _uiState.update { it.copy(otpError = false) }
                delay(400)
                _otpVerified.send(Unit)
                _uiState.update { it.copy(otp = "") }
            } else {
                _uiState.update { it.copy(otpError = true) }
                delay(1200)
                _uiState.update { it.copy(otp = "", otpError = false) }
            }
        }
    }
}
