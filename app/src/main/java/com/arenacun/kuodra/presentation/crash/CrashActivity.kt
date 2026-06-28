package com.arenacun.kuodra.presentation.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arenacun.kuodra.MainActivity
import com.arenacun.kuodra.presentation.theme.Kuodra
import com.arenacun.kuodra.presentation.theme.KuodraTheme

/**
 * Pantalla de crash: muestra el stack trace capturado por [CrashHandler]. No usa Koin ni
 * ViewModels para seguir funcionando aunque la DI haya fallado; solo depende de [KuodraTheme].
 */
class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "Sin información del error."
        setContent {
            KuodraTheme {
                CrashScreen(
                    trace = trace,
                    onRestart = ::restartApp,
                )
            }
        }
    }

    private fun restartApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (launch != null) startActivity(launch)
        finish()
        Runtime.getRuntime().exit(0)
    }

    companion object {
        const val EXTRA_TRACE = "extra_trace"
    }
}

@Composable
private fun CrashScreen(trace: String, onRestart: () -> Unit) {
    val c = Kuodra.colors
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(c.screenBg)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        // Cabecera con tono de error
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(Kuodra.shape.pill).background(c.negTint),
                contentAlignment = Alignment.Center,
            ) { Text("!", style = Kuodra.type.heading, color = c.neg) }
            Column {
                Text("La app se detuvo", style = Kuodra.type.heading, color = c.ink)
                Text("Ocurrió un error inesperado", style = Kuodra.type.caption, color = c.ink3)
            }
        }

        Text(
            "DETALLE DEL ERROR",
            style = Kuodra.type.overline,
            color = c.ink3,
            modifier = Modifier.padding(start = 2.dp, top = 22.dp, bottom = 8.dp),
        )

        // Trace scrollable (vertical + horizontal) en monoespaciado
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(Kuodra.shape.lg)
                .background(c.surface)
                .border(1.dp, c.line, Kuodra.shape.lg)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
        ) {
            Text(
                trace,
                style = Kuodra.type.overline.copy(fontSize = 12.sp, letterSpacing = 0.sp, lineHeight = 18.sp),
                color = c.ink2,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CrashButton("Copiar", filled = false, modifier = Modifier.weight(1f)) {
                copyToClipboard(context, trace)
            }
            CrashButton("Reiniciar", filled = true, modifier = Modifier.weight(1f), onClick = onRestart)
        }
    }
}

@Composable
private fun CrashButton(label: String, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Kuodra.colors
    Box(
        modifier
            .clip(Kuodra.shape.lg)
            .background(if (filled) c.primary else c.surface2)
            .border(1.dp, if (filled) c.primary else c.line, Kuodra.shape.lg)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Kuodra.type.heading, color = if (filled) c.primaryInk else c.ink) }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Kuodra crash", text))
}
