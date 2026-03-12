package com.methodica.app.domain.usecase.subject

import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.repository.SubjectRepository

class GetSubjectUseCase(private val repository: SubjectRepository) {
    suspend operator fun invoke(id: Long): Subject? = repository.getSubject(id)
}
