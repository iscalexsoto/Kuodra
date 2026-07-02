package com.arenacun.kuodra.data.telemetry

import android.content.Context
import android.os.Build
import java.util.Locale

/**
 * Contexto de dispositivo/app que se adjunta a cada evento. Android vive aquí (no en `domain`).
 * [release] usa el mismo formato que la pantalla de crash: `versionName (versionCode)`.
 */
class DeviceContextProvider(private val context: Context) {

    val release: String by lazy {
        runCatching {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pkg.versionName} (${pkg.longVersionCode})"
        }.getOrDefault("desconocida")
    }

    fun snapshot(): Map<String, String> = mapOf(
        "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "android_sdk" to Build.VERSION.SDK_INT.toString(),
        "android_release" to Build.VERSION.RELEASE,
        "locale" to Locale.getDefault().toString(),
    )
}
