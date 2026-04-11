package com.methodica.app.domain.usecase.academic

import com.methodica.app.domain.model.AcademicYear
import com.methodica.app.domain.repository.AcademicCatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveAcademicYearsUseCase(
    private val repository: AcademicCatalogRepository
) {
    operator fun invoke(degreeId: Long): Flow<List<AcademicYear>> =
        repository.observeAcademicYears(degreeId)
}

