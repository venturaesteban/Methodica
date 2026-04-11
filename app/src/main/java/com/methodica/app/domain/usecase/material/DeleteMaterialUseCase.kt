package com.methodica.app.domain.usecase.material

import com.methodica.app.domain.ai.local.LocalAiIngestionPipeline
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.repository.MaterialRepository

class DeleteMaterialUseCase(
    private val repository: MaterialRepository,
    private val localAiIngestionPipeline: LocalAiIngestionPipeline
) {
    suspend operator fun invoke(material: Material) {
        localAiIngestionPipeline.deleteMaterialIndex(material.subjectId, material.id)
        repository.deleteMaterial(material)
    }
}
