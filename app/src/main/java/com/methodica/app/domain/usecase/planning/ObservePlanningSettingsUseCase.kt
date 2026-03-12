package com.methodica.app.domain.usecase.planning

import com.methodica.app.domain.model.PlanningSettings
import com.methodica.app.domain.repository.PlanningSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObservePlanningSettingsUseCase(
    private val repository: PlanningSettingsRepository
) {
    operator fun invoke(): Flow<PlanningSettings> = repository.observeSettings()
}
