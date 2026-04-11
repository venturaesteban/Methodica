package com.methodica.app.domain.usecase.session

import com.methodica.app.domain.model.StudySession
import com.methodica.app.domain.repository.StudySessionRepository
import kotlinx.coroutines.flow.Flow

class ObserveStudySessionsForRangeUseCase(
    private val repository: StudySessionRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<StudySession>> =
        repository.observeByRange(startDate, endDate)
}
