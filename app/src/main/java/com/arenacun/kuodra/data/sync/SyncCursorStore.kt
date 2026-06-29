package com.arenacun.kuodra.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Cursor de deltas por colección (el `updated` máximo ya traído). Persistido en DataStore para
 * que el pull continúe donde quedó entre arranques.
 */
class SyncCursorStore(private val dataStore: DataStore<Preferences>) {

    suspend fun get(collection: String): String =
        dataStore.data.map { it[key(collection)] }.first().orEmpty()

    suspend fun set(collection: String, value: String) {
        dataStore.edit { it[key(collection)] = value }
    }

    private fun key(collection: String) = stringPreferencesKey("cursor_$collection")
}
