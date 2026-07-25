package com.arenacun.kuodra

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.arenacun.kuodra.data.local.SessionStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder

/**
 * DataStore reales sobre archivos temporales para tests de la capa `data`.
 *
 * Sustituye al par `TemporaryFolder` + `PreferenceDataStoreFactory.create { … }`: ese overload usa
 * por defecto un scope que **nunca se cancela**, así que el actor del DataStore sobrevive al test y,
 * si le queda una escritura en vuelo cuando el `TemporaryFolder` ya borró el archivo, falla con
 * `IOException` sin nadie que la capture. `kotlinx-coroutines-test` la recoge en su handler global y
 * la achaca al SIGUIENTE `runTest` (`UncaughtExceptionsBeforeTest`), con lo que falla un test ajeno
 * y de forma intermitente según el orden de clases.
 *
 * Esta regla **posee** la carpeta temporal, de modo que el orden cancelar-scope → borrar-archivos
 * queda garantizado sin depender del orden de reglas de JUnit.
 */
class TestPrefsRule : ExternalResource() {

    private val folder = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun dataStore(name: String = "test.preferences_pb"): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { folder.newFile(name) }

    fun sessionStore(name: String = "test.preferences_pb"): SessionStore = SessionStore(dataStore(name))

    /** Carpeta temporal suelta (p. ej. el directorio del `CrashSpool`). */
    fun newFolder(name: String): File = folder.newFolder(name)

    override fun before() = folder.create()

    override fun after() {
        scope.cancel()
        folder.delete()
    }
}
