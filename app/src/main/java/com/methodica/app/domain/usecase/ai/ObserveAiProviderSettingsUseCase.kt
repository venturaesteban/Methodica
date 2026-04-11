package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.model.AiProviderSettings
import com.methodica.app.domain.repository.AiProviderSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveAiProviderSettingsUseCase(
    private val repository: AiProviderSettingsRepository
) {
    operator fun invoke(): Flow<AiProviderSettings> = repository.observeSettings()
}

