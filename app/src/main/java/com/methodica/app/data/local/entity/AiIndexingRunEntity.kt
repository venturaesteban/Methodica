package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_indexing_runs",
    indices = [Index("subjectId"), Index("assessmentId"), Index("status")]
)
data class AiIndexingRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val assessmentId: Long?,
    val materialId: Long?,
    val status: String,
    val trigger: String,
    val startedAt: Long,
    val completedAt: Long?,
    val errorMessage: String?
)
