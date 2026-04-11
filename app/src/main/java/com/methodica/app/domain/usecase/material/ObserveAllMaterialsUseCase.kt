package com.methodica.app.domain.usecase.material

import com.methodica.app.domain.model.Material
import com.methodica.app.domain.repository.MaterialRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllMaterialsUseCase(
    private val repository: MaterialRepository
) {
    operator fun invoke(): Flow<List<Material>> = repository.observeAllMaterials()
}
