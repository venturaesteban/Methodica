package com.methodica.app.domain.usecase.material

import com.methodica.app.domain.model.Material
import com.methodica.app.domain.repository.MaterialRepository

class DeleteMaterialUseCase(
    private val repository: MaterialRepository
) {
    suspend operator fun invoke(material: Material) = repository.deleteMaterial(material)
}
