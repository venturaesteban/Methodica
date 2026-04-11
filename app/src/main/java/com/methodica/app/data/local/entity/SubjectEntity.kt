package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.Subject

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = AcademicYearEntity::class,
            parentColumns = ["id"],
            childColumns = ["academicYearId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("academicYearId")]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id:          Long    = 0,
    val academicYearId: Long,
    val name:        String,
    val colorHex:    String,
    val description: String? = null
)

data class SubjectWithHierarchyRow(
    val id: Long,
    val academicYearId: Long,
    val degreeId: Long,
    val degreeName: String,
    val courseYear: Int,
    val name: String,
    val colorHex: String,
    val description: String?
)

// --- Mappers entre capa data y capa domain ---

fun SubjectEntity.toDomain(): Subject =
    Subject(
        id = id,
        academicYearId = academicYearId,
        name = name,
        colorHex = colorHex,
        description = description
    )

fun SubjectWithHierarchyRow.toDomain(): Subject =
    Subject(
        id = id,
        academicYearId = academicYearId,
        degreeId = degreeId,
        degreeName = degreeName,
        courseYear = courseYear,
        name = name,
        colorHex = colorHex,
        description = description
    )

fun Subject.toEntity(): SubjectEntity =
    SubjectEntity(
        id = id,
        academicYearId = academicYearId,
        name = name,
        colorHex = colorHex,
        description = description
    )
