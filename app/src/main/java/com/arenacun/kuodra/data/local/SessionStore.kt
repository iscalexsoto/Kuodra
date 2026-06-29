package com.arenacun.kuodra.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.arenacun.kuodra.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persiste la sesión de PocketBase (token + identidad del usuario) en DataStore.
 * El token nunca sale de la capa `data`; al dominio solo expone [Session].
 */
class SessionStore(
    private val dataStore: DataStore<Preferences>,
) {
    val sessionFlow: Flow<Session?> = dataStore.data.map { prefs ->
        val id = prefs[USER_ID]
        val email = prefs[EMAIL]
        val token = prefs[TOKEN]
        if (id != null && email != null && token != null) {
            Session(id, email, prefs[NAME].orEmpty())
        } else {
            null
        }
    }

    /** Token crudo para adjuntar en el header `Authorization`, o `null` si no hay sesión. */
    suspend fun token(): String? = dataStore.data.map { it[TOKEN] }.first()

    /** Id del usuario en sesión, o `null`. Sella el `owner` de los registros locales (Room). */
    suspend fun userId(): String? = dataStore.data.map { it[USER_ID] }.first()

    suspend fun save(token: String, userId: String, email: String, name: String) {
        dataStore.edit {
            it[TOKEN] = token
            it[USER_ID] = userId
            it[EMAIL] = email
            it[NAME] = name
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(TOKEN)
            it.remove(USER_ID)
            it.remove(EMAIL)
            it.remove(NAME)
        }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("session_token")
        val USER_ID = stringPreferencesKey("session_user_id")
        val EMAIL = stringPreferencesKey("session_email")
        val NAME = stringPreferencesKey("session_name")
    }
}
