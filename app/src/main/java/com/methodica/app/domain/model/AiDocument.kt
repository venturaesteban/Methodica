package com.methodica.app.domain.model

data class AiDocument(
    val id: Long = 0,
    val subjectId: Long,
    val assessmentId: Long?,
    val materialId: Long?,
    val sourceType: AiDocumentSourceType,
    val sourceLabel: String,
    val extractedText: String,
    val createdAt: Long = System.currentTimeMillis()
)
