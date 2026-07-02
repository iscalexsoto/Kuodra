package com.arenacun.kuodra.data.telemetry

import com.arenacun.kuodra.data.local.db.TelemetryDao
import com.arenacun.kuodra.data.remote.dto.BreadcrumbDto
import com.arenacun.kuodra.data.remote.dto.TelemetryRecord
import com.arenacun.kuodra.domain.telemetry.LogLevel
import com.arenacun.kuodra.domain.telemetry.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID

/**
 * Implementación de [Telemetry] respaldada en PocketBase (ver plan de observabilidad). Mantiene el
 * ring buffer de breadcrumbs, la identidad del usuario y el `sessionId` del proceso; los eventos van
 * a la cola de Room y se suben con [TelemetryUploader] vía [TelemetryTrigger].
 *
 * Ninguna operación debe propagar excepciones: la telemetría jamás debe tumbar la app.
 */
class PocketBaseTelemetry(
    private val dao: TelemetryDao,
    private val device: DeviceContextProvider,
    private val trigger: TelemetryTrigger,
    private val spool: CrashSpool,
    private val json: Json,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : Telemetry {

    private val buffer = BreadcrumbBuffer()
    private val sessionId = UUID.randomUUID().toString()

    @Volatile private var userId: String = ""
    @Volatile private var userEmail: String = ""

    override fun breadcrumb(category: String, message: String, data: Map<String, String>) {
        buffer.add(BreadcrumbDto(now(), category, message, LogLevel.Info.name, data))
    }

    override fun log(level: LogLevel, message: String, tags: Map<String, String>, throwable: Throwable?) {
        if (level < LogLevel.Warning) {
            breadcrumb("log", message, tags)
            return
        }
        enqueue(type = if (throwable != null) "error" else "log", level = level,
            message = message, throwable = throwable, extra = tags)
    }

    override fun capture(throwable: Throwable, level: LogLevel, context: Map<String, String>) {
        enqueue(type = "error", level = level,
            message = throwable.message ?: throwable.javaClass.simpleName,
            throwable = throwable, extra = context)
    }

    override fun captureFatalBlocking(throwable: Throwable) {
        runCatching {
            val record = buildRecord("crash", LogLevel.Fatal,
                throwable.message ?: throwable.javaClass.simpleName, throwable, emptyMap())
            spool.writeBlocking(record)
        }
    }

    override fun setUser(id: String?, email: String?) {
        userId = id.orEmpty()
        userEmail = email.orEmpty()
        breadcrumb("auth", "setUser", mapOf("hasId" to (!id.isNullOrEmpty()).toString()))
    }

    override fun flush() {
        runCatching { trigger.requestUpload() }
    }

    /** Arma el evento, lo encola (fire-and-forget) y pide subir. Nunca lanza. */
    private fun enqueue(type: String, level: LogLevel, message: String, throwable: Throwable?, extra: Map<String, String>) {
        runCatching {
            val record = buildRecord(type, level, message, throwable, extra)
            scope.launch {
                runCatching { dao.enqueue(record.toEntity(json)) }
            }
            buffer.add(BreadcrumbDto(now(), type, message, level.name, extra))
            trigger.requestUpload()
        }
    }

    private fun buildRecord(
        type: String,
        level: LogLevel,
        message: String,
        throwable: Throwable?,
        extra: Map<String, String>,
    ): TelemetryRecord = TelemetryRecord(
        id = UUID.randomUUID().toString(),
        createdAt = now(),
        level = level.name,
        type = type,
        message = message,
        stacktrace = throwable?.let(::stackTraceOf).orEmpty(),
        breadcrumbs = buffer.snapshot(),
        context = device.snapshot(),
        tags = extra,
        userId = userId,
        userEmail = userEmail,
        release = device.release,
        sessionId = sessionId,
        fingerprint = fingerprintOf(throwable, message),
    )

    private fun stackTraceOf(throwable: Throwable): String =
        StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()

    /** Agrupa eventos similares: clase de la excepción + primer frame, o el mensaje si no hay throwable. */
    private fun fingerprintOf(throwable: Throwable?, message: String): String {
        if (throwable == null) return message.take(120)
        val frame = throwable.stackTrace.firstOrNull()?.let { "${it.className}.${it.methodName}" }.orEmpty()
        return "${throwable.javaClass.name}:$frame"
    }

    private fun now(): Long = System.currentTimeMillis()
}
