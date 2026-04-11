package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.repository.AiAnalysisRepository

class SaveAiAnalysisEditsUseCase(
    private val repository: AiAnalysisRepository
) {
    suspend operator fun invoke(
        analysisId: Long,
        estimatedScope: String,
        justification: String,
        topicComplexities: List<TopicComplexityAnalysis>
    ): Result<Unit> {
        if (estimatedScope.isBlank()) {
            return Result.failure(IllegalArgumentException("El alcance estimado no puede estar vacío"))
        }
        if (justification.isBlank()) {
            return Result.failure(IllegalArgumentException("La justificación no puede estar vacía"))
        }
        return runCatching {
            repository.saveUserEdits(
                analysisId = analysisId,
                estimatedScope = estimatedScope,
                justification = justification,
                topicComplexities = topicComplexities
            )
        }
    }
}
