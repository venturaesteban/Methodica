package com.methodica.app.domain.usecase.assessment

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.repository.AssessmentRepository

class GetAssessmentUseCase(private val repository: AssessmentRepository) {
    suspend operator fun invoke(id: Long): Assessment? = repository.getAssessment(id)
}
