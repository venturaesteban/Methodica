package com.methodica.app.domain.ai.model

data class PlanningRecommendation(
    val recommendedTopicOrder: List<String>,
    val extraReviewTopics: List<String>,
    val warnings: List<String>,
    val confidence: Float
)
