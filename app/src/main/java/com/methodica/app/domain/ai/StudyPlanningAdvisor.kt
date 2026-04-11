package com.methodica.app.domain.ai

import com.methodica.app.domain.ai.model.ComplexityAnalysis
import com.methodica.app.domain.ai.model.ExamScopeInference
import com.methodica.app.domain.ai.model.PlanningRecommendation

interface StudyPlanningAdvisor {
    suspend fun recommend(
        scopeInference: ExamScopeInference,
        complexityAnalysis: ComplexityAnalysis
    ): PlanningRecommendation
}
