package com.arenacun.kuodra.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Único DataStore de preferencias de la app (sesión, tema, espacio activo).
 * Cada repositorio usa sus propias claves sobre este mismo store.
 */
val Context.kuodraDataStore: DataStore<Preferences> by preferencesDataStore(name = "kuodra")
