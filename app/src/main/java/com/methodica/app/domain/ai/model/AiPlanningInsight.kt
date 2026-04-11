package com.methodica.app.domain.ai.model

data class AiPlanningInsight(
    val scopeInference: ExamScopeInference,
    val complexityAnalysis: ComplexityAnalysis,
    val recommendation: PlanningRecommendation,
    val summary: String,
    val confidence: Float,
    val requiresConfirmation: Boolean
)
