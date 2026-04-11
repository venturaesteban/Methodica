package com.methodica.app.domain.ai.model

data class ExamScopeInference(
    val estimatedScope: String,
    val includedTopicNames: List<String>,
    val justification: String,
    val confidence: Float,
    val requiresConfirmation: Boolean
)
