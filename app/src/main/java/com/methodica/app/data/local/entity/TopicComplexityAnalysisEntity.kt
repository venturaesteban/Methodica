package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.TopicComplexityAnalysis

@Entity(
    tableName = "topic_complexity_analysis",
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
data class TopicComplexityAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val analysisId: Long,
    val topicName: String,
    val isIncludedInScope: Boolean,
    val complexityLevel: Int,
    val recommendedHours: Int,
    val priority: Int,
    val requiresPractice: Boolean,
    val requiresSpacedReview: Boolean,
    val rationale: String
)

fun TopicComplexityAnalysisEntity.toDomain(): TopicComplexityAnalysis = TopicComplexityAnalysis(
    id = id,
    analysisId = analysisId,
    topicName = topicName,
    isIncludedInScope = isIncludedInScope,
    complexityLevel = complexityLevel,
    recommendedHours = recommendedHours,
    priority = priority,
    requiresPractice = requiresPractice,
    requiresSpacedReview = requiresSpacedReview,
    rationale = rationale
)

fun TopicComplexityAnalysis.toEntity(): TopicComplexityAnalysisEntity = TopicComplexityAnalysisEntity(
    id = id,
    analysisId = analysisId,
    topicName = topicName,
    isIncludedInScope = isIncludedInScope,
    complexityLevel = complexityLevel,
    recommendedHours = recommendedHours,
    priority = priority,
    requiresPractice = requiresPractice,
    requiresSpacedReview = requiresSpacedReview,
    rationale = rationale
)
