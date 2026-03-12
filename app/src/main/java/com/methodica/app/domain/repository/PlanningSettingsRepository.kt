package com.methodica.app.domain.repository

import com.methodica.app.domain.model.PlanningSettings
import kotlinx.coroutines.flow.Flow

interface PlanningSettingsRepository {
    fun observeSettings(): Flow<PlanningSettings>
    suspend fun saveSettings(settings: PlanningSettings)
}
