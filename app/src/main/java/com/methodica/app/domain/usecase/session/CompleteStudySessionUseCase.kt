package com.methodica.app.domain.usecase.session

import com.methodica.app.domain.model.StudySession
import com.methodica.app.domain.model.StudySessionStatus
import com.methodica.app.domain.repository.StudySessionRepository

class CompleteStudySessionUseCase(
    private val repository: StudySessionRepository
) {
    suspend operator fun invoke(session: StudySession): Result<Unit> = runCatching {
        repository.updateSession(session.copy(status = StudySessionStatus.COMPLETED))
    }
}
