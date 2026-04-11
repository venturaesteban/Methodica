package com.methodica.app.domain.usecase.academic

import com.methodica.app.domain.repository.AcademicCatalogRepository

class EnsureDegreeUseCase(
    private val repository: AcademicCatalogRepository
) {
    suspend operator fun invoke(name: String): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("La titulacion no puede estar vacia"))
        }
        return runCatching { repository.ensureDegree(name) }
    }
}

