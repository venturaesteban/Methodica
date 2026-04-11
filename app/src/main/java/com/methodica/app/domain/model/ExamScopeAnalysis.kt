package com.methodica.app.domain.model

data class ExamScopeAnalysis(
    val id: Long = 0,
    val analysisId: Long,
    val estimatedScope: String,
    val justification: String,
    val confidence: Float,
    val requiresUserConfirmation: Boolean
)
