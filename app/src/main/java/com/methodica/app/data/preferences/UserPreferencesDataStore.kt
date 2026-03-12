package com.methodica.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Punto de acceso único al DataStore de preferencias de usuario.
 * Scope: Application Context → el sistema garantiza una única instancia.
 *
 * IMPORTANTE: DataStore es exclusivamente para preferencias de UI y configuraciones
 * del usuario (tema, notificaciones, etc.). Nunca almacenar datos académicos aquí;
 * esos van en Room.
 */
val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)
