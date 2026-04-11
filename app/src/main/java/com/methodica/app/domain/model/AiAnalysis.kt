package com.methodica.app.domain.model

data class AiAnalysis(
    val id: Long = 0,
    val assessmentId: Long,
    val documentId: Long,
    val summary: String,
    val confidence: Float,
    val requiresConfirmation: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
