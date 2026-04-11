package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.repository.AiAnalysisRepository
import com.methodica.app.domain.repository.StoredAiAnalysis

class GetLatestAiAnalysisForAssessmentUseCase(
    private val repository: AiAnalysisRepository
) {
    suspend operator fun invoke(assessmentId: Long): StoredAiAnalysis? =
        repository.getLatestAnalysisForAssessment(assessmentId)
}
