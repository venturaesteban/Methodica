package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.AiAnalysis

@Entity(
    tableName = "ai_analysis",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AiDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("assessmentId"), Index("documentId")]
)
data class AiAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val documentId: Long,
    val summary: String,
    val confidence: Float,
    val requiresConfirmation: Boolean,
    val createdAt: Long
)

fun AiAnalysisEntity.toDomain(): AiAnalysis = AiAnalysis(
    id = id,
    assessmentId = assessmentId,
    documentId = documentId,
    summary = summary,
    confidence = confidence,
    requiresConfirmation = requiresConfirmation,
    createdAt = createdAt
)

fun AiAnalysis.toEntity(): AiAnalysisEntity = AiAnalysisEntity(
    id = id,
    assessmentId = assessmentId,
    documentId = documentId,
    summary = summary,
    confidence = confidence,
    requiresConfirmation = requiresConfirmation,
    createdAt = createdAt
)
