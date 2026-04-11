package com.methodica.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.methodica.app.domain.model.AiProviderSettings
import com.methodica.app.domain.repository.AiProviderSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiProviderSettingsRepositoryImpl(
    private val context: Context
) : AiProviderSettingsRepository {

    private val dataStore = context.userPreferencesDataStore

    private companion object {
        val KEY_EXTERNAL_ENABLED = booleanPreferencesKey("ai_external_enabled")
        val KEY_PROVIDER_NAME = stringPreferencesKey("ai_provider_name")
        val KEY_BASE_URL = stringPreferencesKey("ai_provider_base_url")
        val KEY_MODEL = stringPreferencesKey("ai_provider_model")
        val KEY_API_KEY = stringPreferencesKey("ai_provider_api_key")
    }

    override fun observeSettings(): Flow<AiProviderSettings> =
        dataStore.data.map { prefs ->
            AiProviderSettings(
                externalEnabled = prefs[KEY_EXTERNAL_ENABLED] ?: false,
                providerName = prefs[KEY_PROVIDER_NAME] ?: "",
                baseUrl = prefs[KEY_BASE_URL] ?: "",
                model = prefs[KEY_MODEL] ?: "",
                apiKey = prefs[KEY_API_KEY] ?: ""
            )
        }

    override suspend fun saveSettings(settings: AiProviderSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_EXTERNAL_ENABLED] = settings.externalEnabled
            prefs[KEY_PROVIDER_NAME] = settings.providerName.trim()
            prefs[KEY_BASE_URL] = settings.baseUrl.trim()
            prefs[KEY_MODEL] = settings.model.trim()
            prefs[KEY_API_KEY] = settings.apiKey.trim()
        }
    }
}

