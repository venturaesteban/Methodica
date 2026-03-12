package com.methodica.app.domain.usecase.assessment

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.repository.AssessmentRepository

class DeleteAssessmentUseCase(private val repository: AssessmentRepository) {

    suspend operator fun invoke(assessment: Assessment) {
        repository.deleteAssessment(assessment)
    }
}
