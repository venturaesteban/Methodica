package com.methodica.app.domain.model

data class TopicComplexityAnalysis(
    val id: Long = 0,
    val analysisId: Long,
    val topicName: String,
    val isIncludedInScope: Boolean,
    val complexityLevel: Int,
    val recommendedHours: Int,
    val priority: Int,
    val requiresPractice: Boolean,
    val requiresSpacedReview: Boolean,
    val rationale: String
)
