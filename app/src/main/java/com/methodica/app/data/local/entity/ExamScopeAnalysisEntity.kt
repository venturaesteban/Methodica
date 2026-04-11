package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.ExamScopeAnalysis

@Entity(
    tableName = "exam_scope_analysis",
    foreignKeys = [
        ForeignKey(
            entity = AiAnalysisEntity::class,
            parentColumns = ["id"],
            childColumns = ["analysisId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("analysisId")]
)
data class ExamScopeAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val analysisId: Long,
    val estimatedScope: String,
    val justification: String,
    val confidence: Float,
    val requiresUserConfirmation: Boolean
)

fun ExamScopeAnalysisEntity.toDomain(): ExamScopeAnalysis = ExamScopeAnalysis(
    id = id,
    analysisId = analysisId,
    estimatedScope = estimatedScope,
    justification = justification,
    confidence = confidence,
    requiresUserConfirmation = requiresUserConfirmation
)

fun ExamScopeAnalysis.toEntity(): ExamScopeAnalysisEntity = ExamScopeAnalysisEntity(
    id = id,
    analysisId = analysisId,
    estimatedScope = estimatedScope,
    justification = justification,
    confidence = confidence,
    requiresUserConfirmation = requiresUserConfirmation
)
