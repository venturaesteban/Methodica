package com.methodica.app.domain.usecase.assessmenttopic

import com.methodica.app.domain.repository.AssessmentTopicRepository

class ReplaceAssessmentTopicsUseCase(
    private val repository: AssessmentTopicRepository
) {
    suspend operator fun invoke(assessmentId: Long, topicIds: List<Long>): Result<Unit> = runCatching {
        repository.replaceTopicsForAssessment(assessmentId, topicIds)
    }
}
