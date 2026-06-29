package com.arenacun.kuodra.domain.model

/**
 * Usuario autenticado. El token de PocketBase vive solo en la capa `data`
 * (no se filtra al dominio); aquí guardamos lo identificable de la sesión.
 */
data class Session(
    val userId: String,
    val email: String,
    /** Nombre con el que el usuario quiere ser identificado; vacío hasta que lo elige en el onboarding. */
    val name: String = "",
)
