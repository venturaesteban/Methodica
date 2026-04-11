package com.methodica.app.domain.model

data class TopicComplexityEstimate(
    val complexityScore: Int,
    val estimatedHours: Int,
    val source: String
)
