package com.methodica.app.feature.planning

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.AiExecutionMode

data class EditableTopicComplexity(
    val id: Long,
    val topicName: String,
    val isIncludedInScope: Boolean,
    val complexityLevel: Int,
    val recommendedHours: Int,
    val priority: Int,
    val requiresPractice: Boolean,
    val requiresSpacedReview: Boolean,
    val rationale: String
)

data class AiAnalysisUiState(
    val isLoading: Boolean = true,
    val isAnalyzing: Boolean = false,
    val isSavingEdits: Boolean = false,
    val isApplyingAndRegenerating: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null,
    val assessments: List<Assessment> = emptyList(),
    val selectedAssessment: Assessment? = null,
    val sourceText: String = "",
    val aiExecutionMode: AiExecutionMode = AiExecutionMode.HEURISTIC,
    val canUseExternalAi: Boolean = false,
    val analysisId: Long? = null,
    val estimatedScope: String = "",
    val justification: String = "",
    val confidence: Float = 0f,
    val requiresConfirmation: Boolean = false,
    val topicEdits: List<EditableTopicComplexity> = emptyList()
)
