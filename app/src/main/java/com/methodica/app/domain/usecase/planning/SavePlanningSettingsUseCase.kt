package com.methodica.app.domain.usecase.planning

import com.methodica.app.domain.model.PlanningSettings
import com.methodica.app.domain.repository.PlanningSettingsRepository

class SavePlanningSettingsUseCase(
    private val repository: PlanningSettingsRepository
) {
    suspend operator fun invoke(settings: PlanningSettings): Result<Unit> = runCatching {
        require(settings.availableHoursPerDay in 1..16) { "Las horas por día deben estar entre 1 y 16." }
        require(settings.bufferDaysBeforeExam >= 0) { "El colchón no puede ser negativo." }
        require(settings.finalReviewDays >= 0) { "Los días de repaso no pueden ser negativos." }
        require(settings.finalReviewDays <= settings.bufferDaysBeforeExam) {
            "Los días de repaso final no pueden superar el colchón."
        }
        require(settings.studentAge in 10..100) { "La edad del estudiante debe estar entre 10 y 100." }
        require(settings.readingComprehensionLevel in 1..5) {
            "La comprensión lectora debe estar entre 1 y 5."
        }
        repository.saveSettings(settings)
    }
}
