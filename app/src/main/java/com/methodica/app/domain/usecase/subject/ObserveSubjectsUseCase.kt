package com.methodica.app.domain.usecase.subject

import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow

class ObserveSubjectsUseCase(private val repository: SubjectRepository) {
    operator fun invoke(): Flow<List<Subject>> = repository.observeSubjects()
}
