package com.methodica.app.domain.usecase.academic

import com.methodica.app.domain.model.Degree
import com.methodica.app.domain.repository.AcademicCatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveDegreesUseCase(
    private val repository: AcademicCatalogRepository
) {
    operator fun invoke(): Flow<List<Degree>> = repository.observeDegrees()
}

