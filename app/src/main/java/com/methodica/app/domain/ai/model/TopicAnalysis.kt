package com.methodica.app.domain.ai.model

data class TopicAnalysis(
    val topicName: String,
    val complexityLevel: Int,
    val estimatedHours: Int,
    val priority: Int,
    val requiresPractice: Boolean,
    val requiresSpacedReview: Boolean,
    val rationale: String,
    val confidence: Float
)
