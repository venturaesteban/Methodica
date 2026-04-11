package com.methodica.app.domain.ai.workflow

import com.methodica.app.domain.model.AiExecutionMode
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.repository.StoredAiAnalysis
import kotlinx.coroutines.flow.Flow

data class AiWorkflowCapability(
    val canUseExternalAi: Boolean,
    val localModelsReady: Boolean,
    val runtimeMessage: String? = null
)

data class AnalyzeAssessmentRequest(
    val assessmentId: Long,
    val rawText: String,
    val sourceLabel: String,
    val executionMode: AiExecutionMode
)

data class ApplyAnalysisEditsRequest(
    val assessmentId: Long,
    val analysisId: Long,
    val estimatedScope: String,
    val justification: String,
    val topicComplexities: List<TopicComplexityAnalysis>
)

interface AiWorkflowCoordinator {
    fun observeCapabilities(): Flow<AiWorkflowCapability>
    suspend fun analyzeAssessment(request: AnalyzeAssessmentRequest): Result<StoredAiAnalysis>
    suspend fun getLatestAnalysis(assessmentId: Long): StoredAiAnalysis?
    suspend fun saveAnalysisEdits(request: ApplyAnalysisEditsRequest): Result<Unit>
    suspend fun applyEditsAndRegenerate(request: ApplyAnalysisEditsRequest): Result<Unit>
}
