package com.methodica.app.domain.usecase.assessmenttopic

import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.repository.AssessmentTopicRepository
import kotlinx.coroutines.flow.Flow

class ObserveAssessmentTopicsUseCase(
    private val repository: AssessmentTopicRepository
) {
    operator fun invoke(assessmentId: Long): Flow<List<Topic>> =
        repository.observeTopicsForAssessment(assessmentId)
}
