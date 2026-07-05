package com.arenacun.kuodra

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arenacun.kuodra.presentation.KuodraRoot
import com.arenacun.kuodra.presentation.feature.auth.OAuthRedirectBus
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    /** Puente hacia la pantalla de auth para el redirect de OAuth2 (login con Google). */
    private val oauthRedirectBus: OAuthRedirectBus by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // La app puede abrirse en frío desde el App Link de redirect (proceso muerto).
        handleOAuthRedirect(intent)
        setContent {
            KuodraRoot()
        }
    }

    // launchMode=singleTop: el redirect reutiliza esta instancia y entra por aquí.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthRedirect(intent)
    }

    /** Extrae `code`/`state` del redirect de OAuth2 y los publica en el bus. */
    private fun handleOAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.path?.startsWith(OAUTH_REDIRECT_PATH) != true) return
        val code = uri.getQueryParameter("code") ?: return
        val state = uri.getQueryParameter("state") ?: return
        oauthRedirectBus.publish(code, state)
    }

    private companion object {
        const val OAUTH_REDIRECT_PATH = "/oauth-redirect"
    }
}
