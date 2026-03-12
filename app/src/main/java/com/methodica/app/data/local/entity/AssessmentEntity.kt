package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.AssessmentType

@Entity(
    tableName   = "assessments",
    foreignKeys = [
        ForeignKey(
            entity        = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns  = ["subjectId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id:        Long    = 0,
    val subjectId: Long,
    val type:      String,   // AssessmentType.name — convertido en mapper
    val title:     String,
    val date:      Long,     // epoch milliseconds
    val weight:    Int?,
    val notes:     String?
)

fun AssessmentEntity.toDomain(): Assessment =
    Assessment(
        id        = id,
        subjectId = subjectId,
        type      = AssessmentType.fromName(type),
        title     = title,
        date      = date,
        weight    = weight,
        notes     = notes
    )

fun Assessment.toEntity(): AssessmentEntity =
    AssessmentEntity(
        id        = id,
        subjectId = subjectId,
        type      = type.name,
        title     = title,
        date      = date,
        weight    = weight,
        notes     = notes
    )
