package com.methodica.app.feature.planning

import com.methodica.app.domain.ai.model.AiPlanningInsight
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.AiExecutionMode
import com.methodica.app.domain.model.PlanningResult
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.model.Topic

data class PlanningUiState(
    val isLoading:          Boolean = true,
    val subjects:           List<Subject> = emptyList(),
    val assessments:        List<Assessment> = emptyList(),
    val selectedAssessment: Assessment? = null,
    val linkedTopics:       List<Topic> = emptyList(),
    val isGenerating:       Boolean = false,
    val aiInputText:        String = "",
    val aiExecutionMode:    AiExecutionMode = AiExecutionMode.HEURISTIC,
    val canUseExternalAi:   Boolean = false,
    val isAnalyzingAi:      Boolean = false,
    val lastAiInsight:      AiPlanningInsight? = null,
    val aiError:            String? = null,
    val lastResult:         PlanningResult? = null,
    val error:              String? = null
)
