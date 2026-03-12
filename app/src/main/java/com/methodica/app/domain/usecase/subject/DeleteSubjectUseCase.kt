package com.methodica.app.domain.usecase.subject

import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.repository.SubjectRepository

class DeleteSubjectUseCase(private val repository: SubjectRepository) {
    suspend operator fun invoke(subject: Subject) = repository.deleteSubject(subject)
}
