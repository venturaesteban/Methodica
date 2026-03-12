package com.methodica.app.domain.usecase.session

import com.methodica.app.domain.model.StudySession
import com.methodica.app.domain.repository.StudySessionRepository
import kotlinx.coroutines.flow.Flow

class ObserveStudySessionsForAssessmentUseCase(
    private val repository: StudySessionRepository
) {
    operator fun invoke(assessmentId: Long): Flow<List<StudySession>> =
        repository.observeByAssessment(assessmentId)
}
