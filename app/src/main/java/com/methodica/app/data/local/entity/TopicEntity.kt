package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.Topic

@Entity(
    tableName    = "topics",
    foreignKeys  = [
        ForeignKey(
            entity        = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns  = ["subjectId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id:             Long = 0,
    val subjectId:      Long,
    val name:           String,
    val difficulty:     Int,
    val estimatedHours: Int,
    val order:          Int
)

fun TopicEntity.toDomain(): Topic =
    Topic(
        id             = id,
        subjectId      = subjectId,
        name           = name,
        difficulty     = difficulty,
        estimatedHours = estimatedHours,
        order          = order
    )

fun Topic.toEntity(): TopicEntity =
    TopicEntity(
        id             = id,
        subjectId      = subjectId,
        name           = name,
        difficulty     = difficulty,
        estimatedHours = estimatedHours,
        order          = order
    )
