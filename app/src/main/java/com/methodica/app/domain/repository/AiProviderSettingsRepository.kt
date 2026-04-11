package com.methodica.app.domain.repository

import com.methodica.app.domain.model.AiProviderSettings
import kotlinx.coroutines.flow.Flow

interface AiProviderSettingsRepository {
    fun observeSettings(): Flow<AiProviderSettings>
    suspend fun saveSettings(settings: AiProviderSettings)
}

