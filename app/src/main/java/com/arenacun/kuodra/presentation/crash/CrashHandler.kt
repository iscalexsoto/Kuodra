package com.arenacun.kuodra.presentation.crash

import android.content.Context
import android.content.Intent
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Manejador global de excepciones no capturadas. En vez de dejar que la app muera en silencio,
 * lanza [CrashActivity] (en su propio proceso, ver el manifest) mostrando el stack trace en
 * pantalla. Pensado para la maqueta: activo en todos los builds.
 *
 * Se instala en [com.arenacun.kuodra.KuodraApplication.onCreate] antes de arrancar Koin, así
 * captura también fallos de cableado de dependencias.
 */
object CrashHandler {

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildReport(appContext, thread, throwable)
                val intent = Intent(appContext, CrashActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra(CrashActivity.EXTRA_TRACE, report)
                appContext.startActivity(intent)
            } catch (_: Throwable) {
                // Si no pudimos mostrar la pantalla, delega al comportamiento por defecto.
                previous?.uncaughtException(thread, throwable)
            } finally {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable): String {
        val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val version = runCatching {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pkg.versionName} (${pkg.longVersionCode})"
        }.getOrDefault("desconocida")

        return buildString {
            appendLine("Kuodra · versión $version")
            appendLine("Hilo: ${thread.name}")
            appendLine("Android SDK ${android.os.Build.VERSION.SDK_INT} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine()
            append(stack)
        }
    }
}
