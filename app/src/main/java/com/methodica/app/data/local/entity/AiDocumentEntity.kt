package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.AiDocument
import com.methodica.app.domain.model.AiDocumentSourceType

@Entity(
    tableName = "ai_documents",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId"), Index("assessmentId"), Index("materialId")]
)
data class AiDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val assessmentId: Long?,
    val materialId: Long?,
    val sourceType: String,
    val sourceLabel: String,
    val extractedText: String,
    val createdAt: Long
)

fun AiDocumentEntity.toDomain(): AiDocument = AiDocument(
    id = id,
    subjectId = subjectId,
    assessmentId = assessmentId,
    materialId = materialId,
    sourceType = runCatching { AiDocumentSourceType.valueOf(sourceType) }.getOrDefault(AiDocumentSourceType.RAW_TEXT),
    sourceLabel = sourceLabel,
    extractedText = extractedText,
    createdAt = createdAt
)

fun AiDocument.toEntity(): AiDocumentEntity = AiDocumentEntity(
    id = id,
    subjectId = subjectId,
    assessmentId = assessmentId,
    materialId = materialId,
    sourceType = sourceType.name,
    sourceLabel = sourceLabel,
    extractedText = extractedText,
    createdAt = createdAt
)
