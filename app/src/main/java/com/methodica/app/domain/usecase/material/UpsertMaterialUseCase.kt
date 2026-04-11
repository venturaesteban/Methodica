package com.methodica.app.domain.usecase.material

import com.methodica.app.domain.ai.local.LocalAiIngestionPipeline
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.repository.MaterialRepository

class UpsertMaterialUseCase(
    private val repository: MaterialRepository,
    private val localAiIngestionPipeline: LocalAiIngestionPipeline
) {
    suspend operator fun invoke(material: Material): Result<Long> {
        if (material.subjectId <= 0L) {
            return Result.failure(IllegalArgumentException("Debes seleccionar una asignatura válida"))
        }
        if (material.title.isBlank()) {
            return Result.failure(IllegalArgumentException("El título del material no puede estar vacío"))
        }
        if (material.uri.isBlank()) {
            return Result.failure(IllegalArgumentException("Debes indicar un enlace o URI de archivo"))
        }
        return runCatching {
            val materialId = repository.saveMaterial(material)
            localAiIngestionPipeline.reindexMaterial(materialId, trigger = "UPSERT_MATERIAL").getOrThrow()
            materialId
        }
    }
}
