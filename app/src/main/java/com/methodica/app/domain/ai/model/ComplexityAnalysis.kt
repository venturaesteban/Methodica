package com.methodica.app.domain.ai.model

data class ComplexityAnalysis(
    val topics: List<TopicAnalysis>,
    val overallComplexity: Int,
    val confidence: Float
)
