package com.methodica.app.domain.usecase.subject

import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.repository.SubjectRepository

class UpsertSubjectUseCase(private val repository: SubjectRepository) {

    suspend operator fun invoke(subject: Subject): Result<Unit> {
        if (subject.name.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre de la materia no puede estar vacío"))
        }
        if (subject.colorHex.isBlank()) {
            return Result.failure(IllegalArgumentException("El color de la materia es obligatorio"))
        }
        return runCatching { repository.saveSubject(subject) }
    }
}
