package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName   = "assessment_topic_cross_ref",
    primaryKeys = ["assessmentId", "topicId"],
    foreignKeys = [
        ForeignKey(
            entity        = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns  = ["assessmentId"],
            onDelete      = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity        = TopicEntity::class,
            parentColumns = ["id"],
            childColumns  = ["topicId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("assessmentId"), Index("topicId")]
)
data class AssessmentTopicCrossRef(
    val assessmentId: Long,
    val topicId:      Long
)
