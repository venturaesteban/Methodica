package com.methodica.app.domain.usecase.academic

import com.methodica.app.domain.repository.AcademicCatalogRepository

class EnsureAcademicYearUseCase(
    private val repository: AcademicCatalogRepository
) {
    suspend operator fun invoke(degreeId: Long, yearNumber: Int): Result<Long> {
        if (degreeId <= 0L) {
            return Result.failure(IllegalArgumentException("Debes seleccionar una titulacion"))
        }
        if (yearNumber <= 0) {
            return Result.failure(IllegalArgumentException("El curso debe ser mayor que 0"))
        }
        return runCatching { repository.ensureAcademicYear(degreeId, yearNumber) }
    }
}

