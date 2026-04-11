package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_document_chunks",
    foreignKeys = [
        ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AssessmentEntity::class, parentColumns = ["id"], childColumns = ["assessmentId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = TopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = MaterialEntity::class, parentColumns = ["id"], childColumns = ["materialId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = AiDocumentEntity::class, parentColumns = ["id"], childColumns = ["documentId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [
        Index("externalId", unique = true),
        Index("subjectId"),
        Index("assessmentId"),
        Index("topicId"),
        Index("materialId"),
        Index("documentId")
    ]
)
data class AiDocumentChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val subjectId: Long,
    val assessmentId: Long?,
    val topicId: Long?,
    val materialId: Long?,
    val documentId: Long?,
    val sequence: Int,
    val sourceLabel: String,
    val content: String,
    val tokenEstimate: Int,
    val contentHash: String,
    val updatedAt: Long
)
