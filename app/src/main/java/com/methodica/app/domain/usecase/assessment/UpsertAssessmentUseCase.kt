package com.methodica.app.domain.usecase.assessment

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.repository.AssessmentRepository

class UpsertAssessmentUseCase(private val repository: AssessmentRepository) {

    suspend operator fun invoke(assessment: Assessment): Result<Long> {
        if (assessment.title.isBlank()) {
            return Result.failure(IllegalArgumentException("El título de la evaluación no puede estar vacío"))
        }
        if (assessment.date <= 0) {
            return Result.failure(IllegalArgumentException("La fecha de la evaluación no es válida"))
        }
        val weight = assessment.weight
        if (weight != null && weight !in 0..100) {
            return Result.failure(IllegalArgumentException("El peso debe estar entre 0 y 100"))
        }
        return runCatching { repository.saveAssessment(assessment) }
    }
}
