package com.methodica.app.domain.repository

import com.methodica.app.domain.model.AiAnalysis
import com.methodica.app.domain.model.AiDocument
import com.methodica.app.domain.model.ExamScopeAnalysis
import com.methodica.app.domain.model.TopicComplexityAnalysis

data class StoredAiAnalysis(
    val document: AiDocument,
    val analysis: AiAnalysis,
    val scope: ExamScopeAnalysis,
    val topicComplexities: List<TopicComplexityAnalysis>
)

interface AiAnalysisRepository {
    suspend fun storeAnalysis(
        document: AiDocument,
        analysis: AiAnalysis,
        scope: ExamScopeAnalysis,
        topicComplexities: List<TopicComplexityAnalysis>
    ): StoredAiAnalysis

    suspend fun getLatestAnalysisForAssessment(assessmentId: Long): StoredAiAnalysis?

    suspend fun saveUserEdits(
        analysisId: Long,
        estimatedScope: String,
        justification: String,
        topicComplexities: List<TopicComplexityAnalysis>
    )
}
