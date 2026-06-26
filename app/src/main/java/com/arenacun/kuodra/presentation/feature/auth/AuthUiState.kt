package com.arenacun.kuodra.presentation.feature.auth

data class AuthUiState(
    val email: String = "",
    val emailValid: Boolean = false,
    val otp: String = "",
    val otpError: Boolean = false,
)
