package com.methodica.app.domain.usecase.material

import com.methodica.app.domain.model.Material
import com.methodica.app.domain.repository.MaterialRepository

class GetMaterialUseCase(
    private val repository: MaterialRepository
) {
    suspend operator fun invoke(id: Long): Material? = repository.getMaterial(id)
}
