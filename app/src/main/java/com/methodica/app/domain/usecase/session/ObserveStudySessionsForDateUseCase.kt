package com.methodica.app.domain.usecase.session

import com.methodica.app.domain.model.StudySession
import com.methodica.app.domain.repository.StudySessionRepository
import kotlinx.coroutines.flow.Flow

class ObserveStudySessionsForDateUseCase(
    private val repository: StudySessionRepository
) {
    operator fun invoke(date: Long): Flow<List<StudySession>> =
        repository.observeByDate(date)
}
