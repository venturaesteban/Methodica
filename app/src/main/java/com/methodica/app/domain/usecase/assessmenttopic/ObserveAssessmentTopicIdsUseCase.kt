package com.methodica.app.domain.usecase.assessmenttopic

import com.methodica.app.domain.repository.AssessmentTopicRepository
import kotlinx.coroutines.flow.Flow

class ObserveAssessmentTopicIdsUseCase(
    private val repository: AssessmentTopicRepository
) {
    operator fun invoke(assessmentId: Long): Flow<List<Long>> =
        repository.observeTopicIdsForAssessment(assessmentId)
}
