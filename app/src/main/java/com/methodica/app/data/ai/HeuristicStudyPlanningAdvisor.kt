package com.methodica.app.data.ai

import com.methodica.app.domain.ai.StudyPlanningAdvisor
import com.methodica.app.domain.ai.model.ComplexityAnalysis
import com.methodica.app.domain.ai.model.ExamScopeInference
import com.methodica.app.domain.ai.model.PlanningRecommendation
import javax.inject.Inject

class HeuristicStudyPlanningAdvisor @Inject constructor() : StudyPlanningAdvisor {

    override suspend fun recommend(
        scopeInference: ExamScopeInference,
        complexityAnalysis: ComplexityAnalysis
    ): PlanningRecommendation {
        val rankedTopics = complexityAnalysis.topics
            .sortedWith(compareByDescending<com.methodica.app.domain.ai.model.TopicAnalysis> { it.priority }
                .thenByDescending { it.complexityLevel })
            .map { it.topicName }

        val extraReviewTopics = complexityAnalysis.topics
            .filter { it.requiresSpacedReview || it.complexityLevel >= 4 }
            .map { it.topicName }

        val warnings = buildList {
            if (scopeInference.requiresConfirmation) {
                add("El alcance inferido tiene confianza media/baja; confirma los temas antes de regenerar el plan.")
            }
            if (extraReviewTopics.isNotEmpty()) {
                add("Se recomienda repaso repetido para: ${extraReviewTopics.joinToString()}")
            }
        }

        val confidence = ((scopeInference.confidence + complexityAnalysis.confidence) / 2f).coerceIn(0f, 1f)

        return PlanningRecommendation(
            recommendedTopicOrder = rankedTopics,
            extraReviewTopics = extraReviewTopics,
            warnings = warnings,
            confidence = confidence
        )
    }
}
