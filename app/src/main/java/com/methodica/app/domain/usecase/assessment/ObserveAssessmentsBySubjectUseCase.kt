package com.methodica.app.domain.usecase.assessment

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.repository.AssessmentRepository
import kotlinx.coroutines.flow.Flow

class ObserveAssessmentsBySubjectUseCase(private val repository: AssessmentRepository) {
    operator fun invoke(subjectId: Long): Flow<List<Assessment>> = repository.observeAssessments(subjectId)
}
