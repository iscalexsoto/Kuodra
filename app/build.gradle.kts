import com.android.build.api.variant.impl.VariantOutputImpl
import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// URL de la instancia PocketBase. Se lee de `local.properties` (clave `pocketbase.url`,
// no versionada); si falta, usa el alias del host local desde el emulador Android.
val pocketbaseUrl: String = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("pocketbase.url") ?: "http://10.0.2.2:8090"

// URL de redirect del OAuth2 (App Link HTTPS). Debe coincidir EXACTAMENTE con el redirect
// registrado en Google Cloud y con el intent-filter de `MainActivity`. Se lee de
// `local.properties` (`oauth.redirect.url=…`, no versionada); si falta, un placeholder de dev.
val oauthRedirectUrl: String = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("oauth.redirect.url") ?: "https://kuodra.app/oauth-redirect"

// Nombre de versión de la app; se reutiliza para nombrar el APK generado.
val appVersionName = "1.0"

android {
    namespace = "com.arenacun.kuodra"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.arenacun.kuodra"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "POCKETBASE_URL", "\"$pocketbaseUrl\"")
        buildConfigField("String", "OAUTH_REDIRECT_URL", "\"$oauthRedirectUrl\"")

        // Host y ruta del App Link de OAuth2, derivados de la URL de redirect, para inyectarlos
        // en el intent-filter del manifest sin duplicar el literal.
        val redirectUri = URI(oauthRedirectUrl)
        manifestPlaceholders["oauthRedirectHost"] = redirectUri.host ?: ""
        manifestPlaceholders["oauthRedirectPath"] = redirectUri.path ?: "/oauth-redirect"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // android.util.Log (SyncManager y otros) no lanza en tests unitarios de host:
        // devuelve valores por defecto en vez de "Method ... not mocked".
        unitTests.isReturnDefaultValues = true
    }
}

// Renombra el APK generado según el tipo de build:
//   release → Kuodra_v{version}.apk
//   debug   → Kuodra_debug_v{version}.apk
androidComponents {
    onVariants { variant ->
        val prefix = if (variant.buildType == "debug") "Kuodra_debug" else "Kuodra"
        variant.outputs.forEach { output ->
            (output as VariantOutputImpl).outputFileName.set("${prefix}_v$appVersionName.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    // Custom Tabs para el flujo OAuth2 (login con Google): abre la pantalla de consentimiento
    // en el navegador del sistema y vuelve a la app por el App Link de redirect.
    implementation(libs.androidx.browser)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Red (Ktor → PocketBase) y persistencia local (DataStore)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.androidx.datastore.preferences)

    // Persistencia local estructurada (Room → fuente de verdad offline)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Sincronización en segundo plano (push/pull diferido y con restricción de red)
    implementation(libs.androidx.work.runtime.ktx)

    // Escaneo de tickets: cámara in-app + OCR on-device (bundled ⇒ funciona offline)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.kotlinx.coroutines.play.services)

    // Inyección de dependencias (Koin)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.koin.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}