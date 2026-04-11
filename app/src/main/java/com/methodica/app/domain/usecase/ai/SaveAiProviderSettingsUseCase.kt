package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.model.AiProviderSettings
import com.methodica.app.domain.repository.AiProviderSettingsRepository

class SaveAiProviderSettingsUseCase(
    private val repository: AiProviderSettingsRepository
) {
    suspend operator fun invoke(settings: AiProviderSettings): Result<Unit> {
        if (settings.providerName.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre del proveedor no puede estar vacío"))
        }
        if (settings.baseUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("La URL base no puede estar vacía"))
        }
        if (settings.model.isBlank()) {
            return Result.failure(IllegalArgumentException("El modelo no puede estar vacío"))
        }
        return runCatching { repository.saveSettings(settings) }
    }
}

