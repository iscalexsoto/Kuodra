package com.arenacun.kuodra.domain.telemetry

/**
 * Puerto neutral de telemetría (observabilidad remota estilo Sentry): breadcrumbs, logs, captura de
 * errores y crashes, e identidad del usuario. Kotlin puro: `domain` no conoce la implementación.
 *
 * El resto de la app **solo** habla con esta interfaz. Cambiar la implementación (PocketBase casero
 * ↔ Sentry ↔ otra) es escribir otra impl en `data` y cambiar **una línea** de binding en Koin, sin
 * tocar pantallas ni repositorios. La impl real es
 * [com.arenacun.kuodra.data.telemetry.PocketBaseTelemetry]; para tests/defaults está [NoOpTelemetry].
 */
interface Telemetry {

    /** Añade una miga a la traza en memoria (no se sube sola; acompaña al próximo evento). */
    fun breadcrumb(category: String, message: String, data: Map<String, String> = emptyMap())

    /**
     * Registra un log. `< Warning` solo deja miga; `>= Warning` genera un evento subible.
     * `throwable` opcional adjunta el stacktrace.
     */
    fun log(
        level: LogLevel,
        message: String,
        tags: Map<String, String> = emptyMap(),
        throwable: Throwable? = null,
    )

    /** Captura una excepción manejada (la app no crashea) con contexto extra. */
    fun capture(throwable: Throwable, level: LogLevel = LogLevel.Error, context: Map<String, String> = emptyMap())

    /**
     * Captura un crash **fatal** de forma síncrona y a prueba de muerte del proceso (persiste a
     * disco antes de morir). Pensado para el manejador de excepciones no capturadas; se sube en el
     * siguiente arranque. No debe lanzar.
     */
    fun captureFatalBlocking(throwable: Throwable)

    /** Fija (o limpia con `null`) la identidad del usuario que acompaña a los eventos. */
    fun setUser(id: String?, email: String?)

    /** Pide intentar subir lo pendiente ahora (p. ej. tras iniciar sesión o al arrancar). */
    fun flush()
}

/** Severidad del evento. Orden natural (por `ordinal`) usable como umbral: Debug < … < Fatal. */
enum class LogLevel { Debug, Info, Warning, Error, Fatal }

/** Implementación vacía: default en constructores y tests que no observan telemetría. */
object NoOpTelemetry : Telemetry {
    override fun breadcrumb(category: String, message: String, data: Map<String, String>) = Unit
    override fun log(level: LogLevel, message: String, tags: Map<String, String>, throwable: Throwable?) = Unit
    override fun capture(throwable: Throwable, level: LogLevel, context: Map<String, String>) = Unit
    override fun captureFatalBlocking(throwable: Throwable) = Unit
    override fun setUser(id: String?, email: String?) = Unit
    override fun flush() = Unit
}
